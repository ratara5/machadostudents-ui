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
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public SortedList<Assignment> sorting(FilteredList<Assignment> filteredAssignments) {

        sortedAssignments = new SortedList<>(filteredAssignments, (Assignment o1, Assignment o2) -> {
            String name1 = o1.getName();
            String name2 = o2.getName();

            // "P" at start priority
            if (name1.startsWith("P") && !name2.startsWith("P")) return -1;
            if (!name1.startsWith("P") && name2.startsWith("P")) return 1;

            // "O" at end priority
            if (name1.startsWith("O") && !name2.startsWith("O")) return 1;
            if (!name1.startsWith("O") && name2.startsWith("O")) return -1;

            // Order number ascending
            boolean name1IsNumber = Character.isDigit(name1.charAt(0));
            boolean name2IsNumber = Character.isDigit(name2.charAt(0));

            if (name1IsNumber && !name2IsNumber) return -1;
            if (!name1IsNumber && name2IsNumber) return 1;

            // Both or none is number, alphabetic order
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

        // Crear las columnas grupales
        TableColumn<Assignment, String> ppalGroupColumn = new TableColumn<>("Ppal");
        ppalGroupColumn.getColumns().addAll(mainPpalColumn, assistantPpalColumn);

        TableColumn<Assignment, String> auxGroupColumn = new TableColumn<>("Aux");
        auxGroupColumn.getColumns().addAll(mainAuxColumn, assistantAuxColumn);

        // Agregar las columnas grupales al TableView
        assignmentTable.getColumns().addAll(ppalGroupColumn, auxGroupColumn);

        assignmentTable.setItems(observableAssignmentList);


        //Get new observable list that includes the columns made with FactoryCell
        ObservableList<Assignment> observableListFull = FXCollections.observableArrayList(assignmentTable.getItems());
        filteredAssignments = new FilteredList<>(observableListFull);
        // Show Assignments for the day 1 of the next month
        Predicate<Assignment> initialFilter = assignment -> {
            return assignment.getDate().isAfter(LocalDate.now().plusMonths(1).withDayOfMonth(1)); //assignment.getDate().getMonth() >= LocalDate.now().getMonth();
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
            return assignment.getDate().isAfter(datePickerStart.getValue()) &&
                    assignment.getDate().isBefore(datePickerEnd.getValue()); //assignment.getDate().getMonth() >= LocalDate.now().getMonth();
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
            PdfMonthlyOverview.generatePDF(groupedData, listStudentsAssignment);

        }

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

            PdfIndividual.fillForms(filteredAssignments, listStudentsAssignment);


            // Convert PDFs in Images
            try {

                String scriptPath = System.getProperty("user.dir") + "/output_scripts/convert_pdfs/pdf_to_image.py";
                String venvPath = System.getProperty("user.dir") + "/output_scripts/convert_pdfs/venv/bin/activate";

                // Comando para ejecutar en la shell
                String command = " source " + venvPath + " && python3.8 " + scriptPath; //python3.8 es venv/bin/python ?

                // Usar ProcessBuilder para ejecutar el comando
                ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);

                // Configurar el directorio de trabajo esperado
                File workingDir = new File(System.getProperty("user.dir")); // Colocar terminal directorio de Java
                System.out.println(workingDir);
                processBuilder.directory(workingDir); // Establecer el directorio de trabajo

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


                int outputCode = process.exitValue();
                if (outputCode == 0) {
                    System.out.println("Script executed succesfully");
                } else {
                    System.out.println("Error in script execution. output code: " + outputCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Exclude weeks without meet
            //int count = (int) assignments.stream()
                    //.filter(obj -> !obj.isWeekWithoutMeet())
                    //.count();

            if(Objects.equals(toCompareCountGenerated, Integer.toString(filteredAssignments.size()))){ //assignments.size()

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

        // Send Images Assignments
        try {
            /*
            String scriptPath = "/home/ratara5/Documents/ideaProjects/machadostudents-ui/machado_ui_scripts/send_assignments/pywhat.py";
            String command = "python3.8 " + scriptPath;
            Process process = Runtime.getRuntime().exec(command);

            // Capture output of process
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            */
            String scriptPath = System.getProperty("user.dir") + "/output_scripts/whatsapp-sender/index.js";
            String authPath = System.getProperty("user.dir") + "/output_scripts/whatsapp-sender/.wwebjs_auth";

            // Comando para ejecutar en la shell
            String command = "WWEBSJS_AUTH_PATH=" + authPath + " node " + scriptPath;

            // Usar ProcessBuilder para ejecutar el comando
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);

            // Configurar el directorio de trabajo esperado
            File workingDir = new File(System.getProperty("user.dir")); // Colocar terminal en el mismo directorio de Java
            processBuilder.directory(workingDir); // Establecer el directorio de trabajo

            Process process = processBuilder.start();
            process.waitFor();

            // Capture output of process
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // Read line by line output of process
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                toCompareCountSent = line;
            }

            process.waitFor();
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