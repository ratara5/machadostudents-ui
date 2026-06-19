package org.machado.machadostudentsui.views;


import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.machado.machadostudentsclient.WebClientMachado;
import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;
import org.machado.machadostudentsui.pdfmanager.PdfIndividual;
import org.machado.machadostudentsui.pdfmanager.PdfMonthlyOverview;
import org.machado.machadostudentsui.utils.SearchUtils;
import org.machado.machadostudentsui.views.common.Dialog;
import org.machado.machadostudentsui.views.popups.AssignmentEdit;
import org.machado.machadostudentsui.views.popups.AssignmentEditAux;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

@Controller
public class Assignments
        extends AbstractController
        implements Consumer<List<Assignment>>,
        Supplier<List<Student>> {

    @FXML
    DatePicker datePickerStart;
    @FXML
    DatePicker datePickerEnd;
    @FXML
    private TableView<Assignment> assignmentTable;
    @FXML
    private VBox addButton;
    @FXML
    private VBox genPdfButton;
    @FXML
    private VBox genAssiButton;
    @FXML
    private VBox sendWappButton;

    @FXML
    private TableColumn<Assignment, LocalDate> dateColumn;
    @FXML
    private TableColumn<Assignment, String> assignmentName;

    private String toCompareCountGenerated;
    private String toCompareCountSent;


    private Assignments controller;
    @Autowired
    private WebClientMachado webClientMachado;

    private FilteredList<Assignment> filteredAssignments;
    private SortedList<Assignment> sortedAssignments;

    private final PdfIndividual pdfIndividual;

    private final PdfMonthlyOverview pdfMonthlyOverview;

    @Autowired
    public Assignments(PdfIndividual pdfIndividual, PdfMonthlyOverview pdfMonthlyOverview) {
        this.pdfIndividual = pdfIndividual;
        this.pdfMonthlyOverview = pdfMonthlyOverview;
    }

    @Value("${python.path.${os.name}}")//@Value("/usr/bin/python3.8")
    String pythonPath;
    @Value("${scriptsPython.path}")
    String scriptsPythonPath;
    @Value("${scriptsJs.path}")
    String scriptsJsPath;

    public SortedList<Assignment> sorting(FilteredList<Assignment> filteredAssignments) {

        sortedAssignments = new SortedList<>(filteredAssignments, (o1, o2) -> {
            String name1 = o1.getName();
            String name2 = o2.getName();

            // "P" at start → highest priority
            boolean p1 = name1.startsWith("P"), p2 = name2.startsWith("P");
            if (p1 != p2) return p1 ? -1 : 1;

            // "O" at start → lowest priority
            boolean o1s = name1.startsWith("O"), o2s = name2.startsWith("O");
            if (o1s != o2s) return o1s ? 1 : -1;

            // Numeric prefix → sort numerically
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

        sortedAssignments.comparatorProperty().bind(assignmentTable.comparatorProperty());
        return sortedAssignments;
    }

    private TableColumn<Assignment, String> createStudentColumn(String columnName, Function<Assignment, Integer> idExtractor) {
        TableColumn<Assignment, String> column = new TableColumn<>(columnName);
        column.setCellValueFactory(cellData -> {
            Assignment assignment = cellData.getValue();
            int studentId = idExtractor.apply(assignment);

            Student student = getStudentById(studentId);
            String name = (student != null) ? String.format("%s %s", student.getName(), student.getLastName()) : "NA";
            return new SimpleStringProperty(name);
        });
        return column;
    }

    @FXML
    public void initialize() throws IOException {

        FXMLLoader loader = new FXMLLoader(Assignments.class.getResource("Assignment.fxml"));
        Assignments controller = (Assignments) loader.getController();


        Mono<List<Assignment>> assignmentsAll = webClientMachado.assignmentsAll();
        ObservableList<Assignment> observableAssignmentList = FXCollections.observableList(assignmentsAll.block());

        TableColumn<Assignment, String> mainPpalColumn = createStudentColumn("mainPpalName", Assignment::getMainStudentPpalId);
        TableColumn<Assignment, String> assistantPpalColumn = createStudentColumn("assistantPpalName", Assignment::getAssistantStudentPpalId);
        TableColumn<Assignment, String> mainAuxColumn = createStudentColumn("mainAuxName", Assignment::getMainStudentAuxId);
        TableColumn<Assignment, String> assistantAuxColumn = createStudentColumn("assistantAuxName", Assignment::getAssistantStudentAuxId);

        // Create group columns
        TableColumn<Assignment, String> ppalGroupColumn = new TableColumn<>("Ppal");
        ppalGroupColumn.getColumns().addAll(mainPpalColumn, assistantPpalColumn);

        TableColumn<Assignment, String> auxGroupColumn = new TableColumn<>("Aux");
        auxGroupColumn.getColumns().addAll(mainAuxColumn, assistantAuxColumn);

        // Add group columns to TableView
        assignmentTable.getColumns().addAll(ppalGroupColumn, auxGroupColumn);


        //Get new observable list that includes the columns made with FactoryCell
        filteredAssignments = new FilteredList<>(observableAssignmentList);

        // Show Assignments for the day 1 of the next month
        Predicate<Assignment> initialFilter = assignment -> {
            return !assignment.getDate().isBefore(LocalDate.now().plusMonths(1).withDayOfMonth(1)); //assignment.getDate().getMonth() >= LocalDate.now().getMonth();
        };
        filteredAssignments.setPredicate(initialFilter);


        //Order
        sortedAssignments = this.sorting(filteredAssignments);
        assignmentTable.getItems().clear();
        assignmentTable.setItems(sortedAssignments);

        /*// Asignar la lista a la TableView
        assignmentTable.setItems(observableAssignmentList);

        // Order by Date Column
        dateColumn.setSortable(true);
        assignmentTable.getSortOrder().add(dateColumn);
        dateColumn.setSortType(TableColumn.SortType.ASCENDING); // or DESCENDING

        // after, order by nameColumn:
        assignmentName.setSortable(true);
        assignmentTable.getSortOrder().add(assignmentName);
        assignmentName.setSortType(TableColumn.SortType.ASCENDING);

        assignmentTable.sort(); // Apply order*/

        MenuItem edit = new MenuItem("Assign Student");
        edit.setOnAction(event -> {
            Assignment assignment = assignmentTable.getSelectionModel().getSelectedItem();
            if(null != assignment) {
                AssignmentEdit.edit(assignment,
                        (Consumer<StudentsAssignment>) this::save,
                        (BiConsumer<String, String>) this::getDeleteStudentAssignmentConsumer,
                        (List<StudentsAssignment>) this.getStudentsAssignmentByAssignment(assignment.getAssignmentId()),
                        (Supplier<List<Student>>) this::get,
                        this::getRoomByIds,


                        (Assignments) controller,
                        (FXMLLoader) loader);
            }
        });
        MenuItem editAux = new MenuItem("Assign Student in Aux Room");
        editAux.setOnAction(event -> {
            Assignment assignment = assignmentTable.getSelectionModel().getSelectedItem();
            if(null != assignment) {
                AssignmentEditAux.edit(assignment,
                        (Consumer<StudentsAssignment>) this::save,
                        (BiConsumer<String, String>) this::getDeleteStudentAssignmentConsumer,
                        (List<StudentsAssignment>) this.getStudentsAssignmentByAssignment(assignment.getAssignmentId()),
                        (Supplier<List<Student>>) this::get,
                        this::getRoomByIds,


                        (Assignments) controller,
                        (FXMLLoader) loader);
            }
        });


        assignmentTable.setContextMenu(new ContextMenu(edit, editAux));

        Tooltip tooltip1 = new Tooltip("Add new Assignment");
        tooltip1.setFont(new Font("Arial", 16));
        Tooltip.install(addButton, tooltip1);

        Tooltip tooltip2 = new Tooltip("Generate Pdf Program");
        tooltip2.setFont(new Font("Arial", 16));
        Tooltip.install(genPdfButton, tooltip2);

        Tooltip tooltip3 = new Tooltip("Generate Pdf Assignments");
        tooltip3.setFont(new Font("Arial", 16));
        Tooltip.install(genAssiButton, tooltip3);

        sendWappButton.setVisible(false);
        sendWappButton.setManaged(false);

    }


    @FXML
    private void search() {

        Predicate<Assignment> searchFilter = assignment -> {
            LocalDate assignmentDate = assignment.getDate();
            LocalDate startDate = datePickerStart.getValue();
            LocalDate endDate = datePickerEnd.getValue();

            return !assignmentDate.isBefore(startDate) && !assignmentDate.isAfter(endDate);
        };
        filteredAssignments.setPredicate(searchFilter);

        sortedAssignments.comparatorProperty().bind(assignmentTable.comparatorProperty());
        sortedAssignments = this.sorting(filteredAssignments);

        assignmentTable.setItems(sortedAssignments);

        //Order by Date Column
        dateColumn.setSortable(true);
        assignmentTable.getSortOrder().add(dateColumn);
        dateColumn.setSortType(TableColumn.SortType.ASCENDING); // or DESCENDING

        assignmentTable.sort(); // Apply order

        sendWappButton.setVisible(false);
        sendWappButton.setManaged(false);

    }


    @FXML
    private void clear() {

        datePickerStart.setValue(null);
        datePickerEnd.setValue(null);

        assignmentTable.getItems().clear();

        sendWappButton.setVisible(false);
        sendWappButton.setManaged(false);

    }


    // Assignments will be loaded in Server when SpringbootApplication will be running
    @FXML
    private void addNew() {
    }


    private void save(StudentsAssignment studentsAssignment) {
        webClientMachado.addStudentAssignment(studentsAssignment);
    }


    public void getDeleteStudentAssignmentConsumer(String studentId, String assignmentId) {
        webClientMachado.deleteStudentAssignment(studentId, assignmentId).subscribe();
    }


    private List<StudentsAssignment> getStudentsAssignmentByAssignment(int assignmentId) {
        List<StudentsAssignment> listStudentsAssignment = webClientMachado.studentsPerAssignment(assignmentId+"").block();
        return listStudentsAssignment;
    }

    private String getRoomByIds(int assignmentId, int mainStudentId) {
        StudentsAssignment studentsAssignment = webClientMachado.studentAssignmentFor(assignmentId+"", mainStudentId+"").block();
        return studentsAssignment !=null ? studentsAssignment.getRoom() : "NA";
    }

    @FunctionalInterface
    public interface RoomProvider {
        String getRoom(int assignmentId, int mainStudentId);
    }

    @Override
    public void accept(List<Assignment> assignments) {
        assignmentTable.getItems().addAll(assignments);
    }


    @Override
    public List<Student> get() {
        List<Student> studentsAllOrderedByOldestAssignment = webClientMachado.studentsOrderedByOldestAssignment().block();
        return studentsAllOrderedByOldestAssignment;
    }


    public Student getStudentById(int studentId) {
        Student studentById = webClientMachado.studentsFor(studentId+"").block();
        return studentById;
    }


    @FXML
    private void generatePdf() {

        if(datePickerStart.getValue() == null || datePickerEnd.getValue() == null){

            Dialog.DialogBuilder.builder()
                    .title("Select period")
                    .message(String.format("Please, select start and end dates"))
                    .build().show();

        } else {

            Predicate<Assignment> searchFilter = assignment -> {
                return assignment.getDate().isAfter(datePickerStart.getValue()) &&
                        assignment.getDate().isBefore(datePickerEnd.getValue()); //assignment.getDate().getMonth() >= LocalDate.now().getMonth();
            };
            filteredAssignments.setPredicate(searchFilter);

            sortedAssignments = this.sorting(filteredAssignments);

            assignmentTable.setItems(sortedAssignments);

            // Order by Date Column
            dateColumn.setSortable(true);
            assignmentTable.getSortOrder().add(dateColumn);
            dateColumn.setSortType(TableColumn.SortType.ASCENDING); // or DESCENDING

            assignmentTable.sort();

            // Grouping data
            Map<LocalDate, Map<String, List<Assignment>>> groupedData = SearchUtils.groupData(sortedAssignments); //filteredAssignments
            // Get ListStudentsAssignments
            List<StudentsAssignment> listStudentsAssignment = webClientMachado.studentsAssignmentAll().block();
            // Generate PDF
            //PdfMonthlyOverview pdfMonthlyOverview = new PdfMonthlyOverview();
            pdfMonthlyOverview.generatePDF(groupedData, listStudentsAssignment);

        }

    }


    public static File getFolder(String outputPath) { // accesses the scriptPath field without needing to pass it as a parameter, since it is an instance field of the class, injected by Spring with the @Value annotation
        String basePath = System.getProperty("user.dir"); // Current working directory
        return new File(basePath, outputPath);  // Combines with the relative path // File ensures the correct separator
    }


    @FXML
    private void generateAssignments() {

        if(datePickerStart.getValue() == null || datePickerEnd.getValue() == null){

            Dialog.DialogBuilder.builder()
                    .title("Select period")
                    .message(String.format("Please, select start and end dates"))
                    .build().show();

        } else {

            // Grouping objects by date
            Map<LocalDate, List<Assignment>> groupedByDate = filteredAssignments.stream()
                    .collect(Collectors.groupingBy(Assignment::getDate));

            // Update objects according condition: If assignment is marked as without meeting date, then the other assignments for this week will be marked as without meeting date
            groupedByDate.forEach((date, assignments) -> {
                boolean anyMarked = assignments.stream().anyMatch(Assignment::isWeekWithoutMeet);
                if (anyMarked) {
                    assignments.forEach(assignment -> assignment.setWeekWithoutMeet(true));
                }
            });

            Predicate<Assignment> searchFilter = assignment -> {
                return assignment.getDate().isAfter(datePickerStart.getValue()) &&
                        assignment.getDate().isBefore(datePickerEnd.getValue()) &&
                        !assignment.isWeekWithoutMeet(); // Without meeting week
            };
            filteredAssignments.setPredicate(searchFilter);
            List<StudentsAssignment> listStudentsAssignment = webClientMachado.studentsAssignmentAll().block();

            //PdfIndividual pdfIndividual = new PdfIndividual();
            pdfIndividual.fillForms(filteredAssignments, listStudentsAssignment);

            File scriptsPythonFolder = getFolder(scriptsPythonPath);
            String scriptsPythonFlexPath = scriptsPythonFolder.getAbsolutePath();


            // Convert PDFs in Images
            try {

                String scriptPath = scriptsPythonFlexPath + File.separator + "pdf_to_image.py"; //System.getProperty("user.dir")  + "/output_scripts/convert_pdfs/pdf_to_image.py";

                // venv according to OS
                String venvPath;
                if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                    venvPath = scriptsPythonFlexPath + File.separator + "venv" + File.separator + "Scripts" + File.separator + "activate";
                } else {
                    venvPath = scriptsPythonFlexPath + File.separator + "venv" + File.separator + "bin" + File.separator + "activate";
                }

                // Command according to OS
                String command;
                if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                    command = "cmd.exe /c " + pythonPath + " " + scriptPath;
                } else {
                    command = "bash -c 'source " + venvPath + " && " + pythonPath + " " + scriptPath + "'";
                }

                // Use ProcessBuilder to execute the command
                ProcessBuilder processBuilder = new ProcessBuilder();
                if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                    processBuilder.command("cmd.exe", "/c", command);
                } else {
                    processBuilder.command("bash", "-c", command);
                }

                // Configure the expected working directory
                File workingDir = new File(System.getProperty("user.dir")); // Place terminal in Java directory
                System.out.println(workingDir);
                processBuilder.directory(workingDir); // Set the working directory

                Process process = processBuilder.start();
                process.waitFor();

                // Capture output of process
                InputStream inputStream = process.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                // Read line by line output of process
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    toCompareCountGenerated = line;
                }


                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    System.out.println("Script executed succesfully");
                } else {
                    System.out.println("Error in script execution. output code: " + exitCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Exclude weeks without meet
            //int count = (int) assignments.stream()
                    //.filter(obj -> !obj.isWeekWithoutMeet())
                    //.count();
            System.out.println("La cuenta de generadas es " + toCompareCountGenerated);
            System.out.println("La cantidad de asignaciones es " + filteredAssignments.size());

            if(Integer.parseInt(toCompareCountGenerated) >= filteredAssignments.size()){ //assignments.size()

                // Unblock SEND TO WHATSAPP Button
                System.out.println("Unblock SEND TO WHATSAPP Button");

                sendWappButton.setVisible(true);
                sendWappButton.setManaged(true);

                Tooltip tooltip4 = new Tooltip("Send Img Assignments");
                tooltip4.setFont(new Font("Arial", 16));
                Tooltip.install(sendWappButton, tooltip4);

            }

        }

    }


    @FXML
    private void sendAssignments() {

        System.out.println("Sending Assignments...");

        File scriptsJsFolder = getFolder(scriptsJsPath);
        String scriptsJsFlexPath = scriptsJsFolder.getAbsolutePath();

        // Send Images Assignments
        try {

            //Find node location on linux
            ////Command according to OS
            List<String> commandSearchNode;
            if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                commandSearchNode = Arrays.asList("cmd.exe", "/c", "where node");
            } else {
                commandSearchNode = Arrays.asList("bash", "-c", "which node");
            }

            ////Use ProcessBuilder to execute the command
            ProcessBuilder checkNode = new ProcessBuilder(commandSearchNode);
            checkNode.redirectErrorStream(true); // Also capture errors
            Process nodeProcess = checkNode.start();

            ////Capturar salidas del proceso
            InputStream nodeInputStream = nodeProcess.getInputStream();
            BufferedReader nodeReader = new BufferedReader(new InputStreamReader(nodeInputStream));

            ////Read process output
            String nodePath = nodeReader.readLine(); // Only reads the first line
            int exitCode = nodeProcess.waitFor();

            ////Validar salida
            if (exitCode != 0 || nodePath == null || nodePath.isEmpty()) {
                // Fallback si no se encuentra 'node'
                if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("lin")) {
                    nodePath = "~/.nvm/versions/node/v20.11.0/bin/node";
                } else {
                    nodePath = "C:\\Program Files\\nvm\\v20.11.0\\node.exe";
                }
                System.out.println("Node.js path fallback: " + nodePath);
            } else {
                System.out.println("Node.js is located at: " + nodePath);
            }


            System.out.println("The user.dir is: " + System.getProperty("user.dir"));
            //JS script to send images

            String scriptPath = scriptsJsFlexPath + File.separator + "index.js"; //System.getProperty("user.dir") + "/output_scripts/whatsapp-sender/index.js";
            String authPath = scriptsJsFlexPath + File.separator + ".wwebjs_auth"; //System.getProperty("user.dir") + "/output_scripts/whatsapp-sender/.wwebjs_auth";

            ////Command to execute in the shell
            String commandExecuteNode = "WWEBJS_AUTH_PATH=" + authPath + " " + nodePath + " " + scriptPath; //

            ////Use ProcessBuilder to execute the command
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                processBuilder.command("cmd.exe", "/c", commandExecuteNode);
            } else {
                processBuilder.command("bash", "-c", commandExecuteNode);
            }

            ////Asignar node al Path del Process
            Map<String, String> environment = processBuilder.environment();
            String separator = File.pathSeparator; // Detecta el separador del sistema
            String currentPath = environment.get("PATH");
            if (currentPath == null) {
                currentPath = "";
            }
            environment.put("PATH", currentPath + separator + nodePath);

            ////Configure the expected working directory
            File workingDir = new File(System.getProperty("user.dir")); // Place terminal in the same Java directory
            processBuilder.directory(workingDir); // Set the working directory

            Process process = processBuilder.start();
            process.waitFor();

            ////Capture output of process
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            ////Read line by line output of process
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                toCompareCountSent = line;
            }

            int outputCode = process.exitValue();
            if (outputCode == 0) {
                System.out.println("Script executed succesfully");
            } else {
                System.out.println("Error in script execution. output code: " + outputCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(Objects.equals(toCompareCountSent, toCompareCountGenerated)){
            // Unblock SEND TO WHATSAPP Button
            System.out.println("All assignments images have been sent");
        }

        sendWappButton.setVisible(false);
        sendWappButton.setManaged(false);

    }


    public static void show(Node element){

        try {
            Stage stage = (Stage) element.getScene().getWindow();
            Parent root = FXMLLoader.load(Assignments.class.getResource("Assignment.fxml"));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @FXML
    public void close(){
        datePickerStart.getScene().getWindow().hide();
    }


    public Assignments getController() {
        return controller;
    }


    public void setController(Assignments controller) {
        this.controller = controller;
    }

}