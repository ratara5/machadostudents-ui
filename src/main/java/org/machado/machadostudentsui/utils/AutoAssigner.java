package org.machado.machadostudentsui.utils;

import org.machado.machadostudentsclient.WebClientMachado;
import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AutoAssigner {

    private static final int ESTUDIANTE = 1;
    private static final int PUBLICADOR = 2;
    private static final int BAUTIZADO = 3;
    private static final int PRECURSOR = 4;
    private static final int SIERVO = 5;
    private static final int ANCIANO = 6;

    private static final List<Integer> ROLE_DISTRIBUTION = List.of(
            ESTUDIANTE, ESTUDIANTE,
            PUBLICADOR, PUBLICADOR,
            BAUTIZADO, BAUTIZADO,
            PRECURSOR, PRECURSOR,
            ANCIANO,
            SIERVO
    );

    private static final String S_TESOROS = "TESOROS DE LA BIBLIA";
    private static final String S_SMM = "SEAMOS MEJORES MAESTROS";
    private static final String S_NUVICRI = "NUESTRA VIDA CRISTIANA";
    private static final String S_PRESIDENTE = "PRESIDENTE";
    private static final String S_ORACION = "ORACIÓN FINAL";

    private static final String PPAL = "Ppal";
    private static final String AUX = "Aux";
    private static final String MAIN = "mainStudent";
    private static final String ASSISTANT = "assistantStudent";

    private final WebClientMachado client;
    private final Random random = new Random();

    public AutoAssigner(WebClientMachado client) {
        this.client = client;
    }

    public void autoAssign(List<Assignment> assignments) {
        List<Assignment> sorted = sortAssignments(assignments);
        clearAllForAssignments(sorted);
        Set<LocalDate> datesWithAux = computeDatesWithAux(assignments);
        List<Student> orderedStudents = loadStudents();
        Map<LocalDate, Set<Integer>> usedPerDate = new HashMap<>();
        OncePerMonthTracker smmTracker = new OncePerMonthTracker();

        List<StudentsAssignment> allSA = client.studentsAssignmentAll().block();
        List<Assignment> allAssignments = client.assignmentsAll().block();
        Map<Integer, Assignment> assignmentMap = allAssignments.stream()
                .collect(Collectors.toMap(Assignment::getAssignmentId, a -> a));

        Map<Integer, LocalDate> presidenteHistory = buildRotationHistory(allSA, assignmentMap,
                sa -> {
                    Assignment a = assignmentMap.get(sa.getAssignmentId());
                    return a != null && S_PRESIDENTE.equals(a.getSection().trim());
                });

        Map<Integer, LocalDate> estudioHistory = buildRotationHistory(allSA, assignmentMap,
                sa -> {
                    Assignment a = assignmentMap.get(sa.getAssignmentId());
                    if (a == null) return false;
                    String section = a.getSection().trim();
                    String name = a.getName();
                    return S_NUVICRI.equals(section)
                            && (name.contains("Estudio b\u00edblico") || name.contains("Estudio biblico"));
                });

        Set<Integer> smmUsedThisMonth = new HashSet<>();
        Set<Integer> presidenteUsedThisPeriod = new HashSet<>();

        for (Assignment a : sorted) {
            if (a.isWeekWithoutMeet()) continue;
            LocalDate date = a.getDate();
            usedPerDate.putIfAbsent(date, new HashSet<>());
            Set<Integer> usedToday = usedPerDate.get(date);
            String section = a.getSection().trim();
            String name = a.getName();
            boolean hasAux = datesWithAux.contains(date);

            if (S_TESOROS.equals(section)) {
                assignTesoros(a, orderedStudents, usedToday);
            } else if (S_SMM.equals(section)) {
                assignSmm(a, orderedStudents, usedToday, hasAux, smmTracker, smmUsedThisMonth);
            } else if (S_NUVICRI.equals(section)) {
                assignNuviCri(a, orderedStudents, usedToday, estudioHistory);
            } else if (S_PRESIDENTE.equals(section) && hasAux) {
                assignPresidente(a, orderedStudents, usedToday, presidenteHistory, presidenteUsedThisPeriod);
            } else if (S_ORACION.equals(section)) {
                assignOracion(a, orderedStudents, usedToday);
            }
        }
    }

    // ===================== SORTING =====================

    private List<Assignment> sortAssignments(List<Assignment> assignments) {
        Map<LocalDate, List<Assignment>> byDate = assignments.stream()
                .collect(Collectors.groupingBy(Assignment::getDate, TreeMap::new, Collectors.toList()));
        List<Assignment> result = new ArrayList<>();
        for (List<Assignment> list : byDate.values()) {
            list.sort((o1, o2) -> {
                String name1 = o1.getName();
                String name2 = o2.getName();
                boolean p1 = name1.startsWith("P"), p2 = name2.startsWith("P");
                if (p1 != p2) return p1 ? -1 : 1;
                boolean o1s = name1.startsWith("O"), o2s = name2.startsWith("O");
                if (o1s != o2s) return o1s ? 1 : -1;
                boolean n1 = Character.isDigit(name1.charAt(0));
                boolean n2 = Character.isDigit(name2.charAt(0));
                if (n1 && n2) {
                    int num1 = Integer.parseInt(name1.replaceAll("^(\\d+).*", "$1"));
                    int num2 = Integer.parseInt(name2.replaceAll("^(\\d+).*", "$1"));
                    if (num1 != num2) return Integer.compare(num1, num2);
                } else if (n1 != n2) {
                    return n1 ? -1 : 1;
                }
                return name1.compareTo(name2);
            });
            result.addAll(list);
        }
        return result;
    }

    private Set<LocalDate> computeDatesWithAux(List<Assignment> assignments) {
        return assignments.stream()
                .filter(a -> S_SMM.equals(a.getSection().trim()))
                .filter(a -> isEvenWeek(a.getDate()))
                .map(Assignment::getDate)
                .collect(Collectors.toSet());
    }

    private boolean isEvenWeek(LocalDate date) {
        return ((date.getDayOfMonth() - 1) / 7 + 1) % 2 == 0;
    }

    // ===================== DB OPERATIONS =====================

    private void clearAllForAssignments(List<Assignment> assignments) {
        for (Assignment a : assignments) {
            List<StudentsAssignment> existing = client.studentsPerAssignment(String.valueOf(a.getAssignmentId())).block();
            if (existing == null) continue;
            for (StudentsAssignment sa : existing) {
                client.deleteStudentAssignment(
                        String.valueOf(sa.getStudentId()),
                        String.valueOf(sa.getAssignmentId())
                ).block();
            }
        }
    }

    private List<Student> loadStudents() {
        return client.studentsOrderedByOldestAssignment().block();
    }

    private Map<Integer, LocalDate> buildRotationHistory(List<StudentsAssignment> allSA,
                                                          Map<Integer, Assignment> assignmentMap,
                                                          Predicate<StudentsAssignment> filter) {
        Map<Integer, LocalDate> result = new HashMap<>();
        for (StudentsAssignment sa : allSA) {
            if (!filter.test(sa)) continue;
            Assignment a = assignmentMap.get(sa.getAssignmentId());
            if (a == null) continue;
            LocalDate date = a.getDate();
            result.merge(sa.getStudentId(), date,
                    (existing, incoming) -> existing.isAfter(incoming) ? existing : incoming);
        }
        return result;
    }

    // ===================== TESOROS DE LA BIBLIA =====================

    private void assignTesoros(Assignment a, List<Student> orderedStudents, Set<Integer> usedToday) {
        String name = a.getName();
        Set<Integer> allowedRoles;
        if (name.startsWith("1")) {
            allowedRoles = Set.of(ANCIANO);
        } else if (name.startsWith("2")) {
            allowedRoles = rollProbability(0.6) ? Set.of(SIERVO) : Set.of(ANCIANO);
        } else {
            allowedRoles = Set.of(ANCIANO);
        }
        assignSingleStudent(a, PPAL, MAIN, orderedStudents, usedToday, allowedRoles, null);
    }

    // ===================== SEAMOS MEJORES MAESTROS =====================

    private void assignSmm(Assignment a, List<Student> orderedStudents, Set<Integer> usedToday,
                            boolean hasAux, OncePerMonthTracker tracker,
                            Set<Integer> smmUsedThisMonth) {
        String name = a.getName();
        boolean isDiscurso = name.contains("Discurso");
        boolean isEmpiece = name.contains("Empiece conversaciones");

        if (isDiscurso) {
            Integer id = assignSingleStudent(a, PPAL, MAIN, orderedStudents, usedToday,
                    Set.of(ANCIANO, SIERVO), "H");
            if (id != null) smmUsedThisMonth.add(id);
            if (hasAux) {
                Integer auxId = assignSingleStudent(a, AUX, MAIN, orderedStudents, usedToday,
                        Set.of(ANCIANO, SIERVO), "H");
                if (auxId != null) smmUsedThisMonth.add(auxId);
            }
        } else if (isEmpiece) {
            assignSmmTwoStudents(a, PPAL, orderedStudents, usedToday, true, tracker, smmUsedThisMonth);
            if (hasAux) {
                assignSmmTwoStudents(a, AUX, orderedStudents, usedToday, true, tracker, smmUsedThisMonth);
            }
        } else {
            assignSmmTwoStudents(a, PPAL, orderedStudents, usedToday, false, tracker, smmUsedThisMonth);
            if (hasAux) {
                assignSmmTwoStudents(a, AUX, orderedStudents, usedToday, false, tracker, smmUsedThisMonth);
            }
        }
    }

    private void assignSmmTwoStudents(Assignment a, String room, List<Student> orderedStudents,
                                       Set<Integer> usedToday, boolean isEmpiece,
                                       OncePerMonthTracker tracker,
                                       Set<Integer> smmUsedThisMonth) {
        for (int attempt = 0; attempt < 30; attempt++) {
            int mainRole = pickFromDistribution();
            Student main = orderedStudents.stream()
                    .filter(s -> !usedToday.contains(s.getStudentId()))
                    .filter(s -> !smmUsedThisMonth.contains(s.getStudentId()))
                    .filter(s -> s.getRoleId() == mainRole)
                    .findFirst().orElse(null);
            if (main == null) continue;

            usedToday.add(main.getStudentId());

            for (int asstAttempt = 0; asstAttempt < 20; asstAttempt++) {
                int asstRole = pickFromDistribution();
                if (!isValidCombo(mainRole, asstRole, tracker)) continue;

                String genderFilter = isEmpiece ? null : main.getGenre();
                Student asst = orderedStudents.stream()
                        .filter(s -> !usedToday.contains(s.getStudentId()))
                        .filter(s -> !smmUsedThisMonth.contains(s.getStudentId()))
                        .filter(s -> s.getRoleId() == asstRole)
                        .filter(s -> genderFilter == null || genderFilter.equals(s.getGenre()))
                        .findFirst().orElse(null);
                if (asst == null) continue;

                if (isEmpiece && !main.getGenre().equals(asst.getGenre())
                        && !main.getLastName().equals(asst.getLastName())) continue;

                save(a, main, room, MAIN);
                save(a, asst, room, ASSISTANT);
                usedToday.add(asst.getStudentId());
                smmUsedThisMonth.add(main.getStudentId());
                smmUsedThisMonth.add(asst.getStudentId());
                return;
            }

            usedToday.remove(main.getStudentId());
        }
    }

    private int pickFromDistribution() {
        return ROLE_DISTRIBUTION.get(random.nextInt(ROLE_DISTRIBUTION.size()));
    }

    private boolean isValidCombo(int role1, int role2, OncePerMonthTracker tracker) {
        int a = role1;
        int b = role2;

        if (a == b && (a == ESTUDIANTE || a == PUBLICADOR)) return false;

        if ((a == PRECURSOR && (b == PRECURSOR || b == ANCIANO || b == SIERVO)) ||
                (b == PRECURSOR && (a == PRECURSOR || a == ANCIANO || a == SIERVO))) {
            return tracker.tryPrecursorCombo();
        }

        if ((a == ANCIANO && (b == ANCIANO || b == SIERVO)) ||
                (b == ANCIANO && (a == ANCIANO || a == SIERVO))) {
            return tracker.tryAncianoCombo();
        }

        if ((isPasRole(a) && isEpbRole(b)) || (isPasRole(b) && isEpbRole(a))) return true;

        if ((a == BAUTIZADO && (b == ESTUDIANTE || b == PUBLICADOR)) ||
                (b == BAUTIZADO && (a == ESTUDIANTE || a == PUBLICADOR))) return true;

        if (a == BAUTIZADO && b == BAUTIZADO) return true;

        return false;
    }

    private boolean isPasRole(int role) {
        return role == PRECURSOR || role == ANCIANO || role == SIERVO;
    }

    private boolean isEpbRole(int role) {
        return role == ESTUDIANTE || role == PUBLICADOR || role == BAUTIZADO;
    }

    // ===================== NUESTRA VIDA CRISTIANA =====================

    private void assignNuviCri(Assignment a, List<Student> orderedStudents, Set<Integer> usedToday,
                                Map<Integer, LocalDate> estudioHistory) {
        String name = a.getName();
        boolean isEstudioBiblico = name.contains("Estudio b\u00edblico") || name.contains("Estudio biblico");

        if (isEstudioBiblico) {
            assignEstudioBiblico(a, orderedStudents, usedToday, estudioHistory);
        } else {
            Set<Integer> roles = rollProbability(0.8) ? Set.of(ANCIANO) : Set.of(SIERVO);
            assignSingleStudent(a, PPAL, MAIN, orderedStudents, usedToday, roles, null);
        }
    }

    private void assignEstudioBiblico(Assignment a, List<Student> orderedStudents,
                                       Set<Integer> usedToday,
                                       Map<Integer, LocalDate> history) {
        Student main = orderedStudents.stream()
                .filter(s -> !usedToday.contains(s.getStudentId()))
                .filter(s -> s.getRoleId() == ANCIANO && "H".equals(s.getGenre()))
                .min(Comparator.comparing(s -> history.getOrDefault(s.getStudentId(), LocalDate.MIN)))
                .orElse(null);
        if (main == null) return;
        save(a, main, PPAL, MAIN);
        usedToday.add(main.getStudentId());

        Student assistant = orderedStudents.stream()
                .filter(s -> !usedToday.contains(s.getStudentId()))
                .filter(s -> (s.getRoleId() == ANCIANO || s.getRoleId() == SIERVO || s.getRoleId() == BAUTIZADO)
                        && "H".equals(s.getGenre()))
                .min(Comparator.comparing(s -> history.getOrDefault(s.getStudentId(), LocalDate.MIN)))
                .orElse(null);
        if (assistant != null) {
            save(a, assistant, PPAL, ASSISTANT);
            usedToday.add(assistant.getStudentId());
        }
    }

    // ===================== PRESIDENTE =====================

    private void assignPresidente(Assignment a, List<Student> orderedStudents, Set<Integer> usedToday,
                                   Map<Integer, LocalDate> history,
                                   Set<Integer> presidenteUsedThisPeriod) {
        Student selected = orderedStudents.stream()
                .filter(s -> !usedToday.contains(s.getStudentId()))
                .filter(s -> !presidenteUsedThisPeriod.contains(s.getStudentId()))
                .filter(s -> s.getRoleId() == ANCIANO)
                .min(Comparator.comparing(s -> history.getOrDefault(s.getStudentId(), LocalDate.MIN)))
                .orElse(null);
        if (selected != null) {
            save(a, selected, PPAL, MAIN);
            usedToday.add(selected.getStudentId());
            presidenteUsedThisPeriod.add(selected.getStudentId());
        }
    }

    // ===================== ORACIÓN FINAL =====================

    private void assignOracion(Assignment a, List<Student> orderedStudents, Set<Integer> usedToday) {
        Set<Integer> roles = rollProbability(0.5) ? Set.of(ANCIANO, SIERVO) : Set.of(BAUTIZADO);
        assignSingleStudent(a, PPAL, MAIN, orderedStudents, usedToday, roles, "H");
    }

    // ===================== COMMON METHODS =====================

    private Integer assignSingleStudent(Assignment a, String room, String roleStudent,
                                         List<Student> orderedStudents, Set<Integer> usedToday,
                                         Set<Integer> allowedRoles, String gender) {
        List<Student> candidates = filterCandidates(orderedStudents, usedToday, allowedRoles, gender);
        if (!candidates.isEmpty()) {
            Student selected = candidates.get(0);
            save(a, selected, room, roleStudent);
            usedToday.add(selected.getStudentId());
            return selected.getStudentId();
        }
        return null;
    }

    private List<Student> filterCandidates(List<Student> orderedStudents, Set<Integer> usedToday,
                                            Set<Integer> allowedRoles, String gender) {
        return orderedStudents.stream()
                .filter(s -> !usedToday.contains(s.getStudentId()))
                .filter(s -> allowedRoles == null || allowedRoles.contains(s.getRoleId()))
                .filter(s -> gender == null || gender.equals(s.getGenre()))
                .collect(Collectors.toList());
    }

    // ===================== HELPERS =====================

    private boolean rollProbability(double probability) {
        return random.nextDouble() < probability;
    }

    private void save(Assignment assignment, Student student, String room, String roleStudent) {
        StudentsAssignment sa = new StudentsAssignment(
                assignment.getAssignmentId(),
                student.getStudentId(),
                roleStudent,
                room
        );
        client.addStudentAssignment(sa);
    }

    private static class OncePerMonthTracker {
        private boolean precursorComboUsed;
        private boolean ancianoComboUsed;

        boolean tryPrecursorCombo() {
            if (precursorComboUsed) return false;
            precursorComboUsed = true;
            return true;
        }

        boolean tryAncianoCombo() {
            if (ancianoComboUsed) return false;
            ancianoComboUsed = true;
            return true;
        }
    }
}
