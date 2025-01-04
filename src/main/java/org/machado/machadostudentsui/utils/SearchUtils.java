package org.machado.machadostudentsui.utils;

import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearchUtils {

    private Consumer<Student> getOneHandler;
    public static Map<LocalDate, Map<String, List<Assignment>>> groupData(List<Assignment> assignments) {

        Map<LocalDate, Map<String, List<Assignment>>> groupedData = new LinkedHashMap<>();
        for (Assignment assignment : assignments) {
            // Group by date
            LocalDate date = assignment.getDate();
            groupedData.putIfAbsent(date, new LinkedHashMap<>());

            // Group by section in each date
            String section = assignment.getSection();
            groupedData.get(date).putIfAbsent(section, new ArrayList<>());

            // Add record to list
            groupedData.get(date).get(section).add(assignment);
        }
        return groupedData;

    }

    public static Map<String, List<StudentsAssignment>> getStudentsAssignmentsByRoom(Assignment assignment, List<StudentsAssignment> listStudentsAssignment) {

        // Filter by assignment id
        List<StudentsAssignment> filteredListStudentsAssignment = listStudentsAssignment
                .stream()
                .filter(x -> x.getAssignmentId() == assignment.getAssignmentId())
                .collect(Collectors.toList());

        // Filter by Ppal room
        List<StudentsAssignment> listStudentsAssignmentPpal = filteredListStudentsAssignment
                .stream()
                .filter(x -> "Ppal".equals(x.getRoom()))
                .collect(Collectors.toList());

        // Filter by Aux room
        List<StudentsAssignment> listStudentsAssignmentAux = filteredListStudentsAssignment
                .stream()
                .filter(x -> "Aux".equals(x.getRoom()))
                .collect(Collectors.toList());

        // Return results on a map
        Map<String, List<StudentsAssignment>> result = new HashMap<>();
        result.put("Ppal", listStudentsAssignmentPpal);
        result.put("Aux", listStudentsAssignmentAux);

        return result;
    }

    public static Optional<Student> getStudentInAssignment (List<StudentsAssignment> listStudentAssignmentRoom, String rolStudent) {

        Optional<Student> studentInAssignment = listStudentAssignmentRoom.stream()
                .filter(student -> rolStudent.equals(student.getRolStudent()))
                .map(StudentsAssignment::getStudent)
                .findFirst();

        return studentInAssignment;
    }

}
