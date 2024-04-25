package org.machado.machadostudentsui.views.popups;


import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import org.machado.machadostudentsclient.WebClientMachado;
import org.machado.machadostudentsclient.entity.*;
import org.machado.machadostudentsui.utils.FormatUtils;
import org.machado.machadostudentsui.utils.Menu;
import org.machado.machadostudentsui.views.Assignments;
import org.machado.machadostudentsui.views.MainFrame;
import org.machado.machadostudentsui.views.common.Dialog;

import jakarta.annotation.PostConstruct;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AssignmentEdit {

    @FXML
    private TableView<Student> studentForAssignmentTable;
    @FXML
    private Label title;
    @FXML
    private GridPane studentsGridPane;
    @FXML
    private Label studentNameLabel;
    @FXML
    private Label assistantNameLabel;
    @FXML
    private TextField name;
    @FXML private ToggleGroup genderToggleGroup;
    @FXML
    private Slider rolSlider; //could be ageSlider
    @FXML
    private Label sliderValueLabel;

    private Assignment assignment;
    private StudentsAssignment studentsAssignment;
    private Consumer<StudentsAssignment> saveHandler;
    private BiConsumer<String, String> deleteHandler;
    private Assignments assignmentController;
    private FXMLLoader assignmentLoader;
    private List<StudentsAssignment> listStudentsAssignment;
    private FilteredList<Student> filteredStudents;
    private List<Integer> studentIdList;
    private int draggedStudentId;
    private LinkedList<Integer> candidatesIdsToDeleteList = new LinkedList<>();
    private boolean flagCandidatesToDeleteFromStudent = false;
    private boolean flagCandidatesToDeleteFromAssistant = false;

    public static void edit(Assignment assignment,
                            Consumer<StudentsAssignment> saveHandler,
                            BiConsumer<String, String> deleteHandler,
                            List<StudentsAssignment> listStudentsAssignment,
                            Supplier<List<Student>> supplier,
                            Assignments assignmentController,
                            FXMLLoader assignmentLoader) {

        try {
            Stage stage = new Stage(StageStyle.UNDECORATED);
            FXMLLoader loader = new FXMLLoader(AssignmentEdit.class.getResource("AssignmentEdit.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);

            AssignmentEdit controller = loader.getController();
            controller.init(assignment,
                    saveHandler,
                    deleteHandler,
                    listStudentsAssignment,
                    supplier,
                    assignmentController,
                    assignmentLoader);
            stage.sizeToScene();
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void init(
            Assignment assignment,
            Consumer<StudentsAssignment> saveHandler,
            BiConsumer<String, String> deleteHandler,
            List<StudentsAssignment> listStudentsAssignment,
            Supplier<List<Student>> supplier,
            Assignments assignmentController,
            FXMLLoader assignmentLoader){

        // 1. Initialize variables
        this.assignment = assignment;
        this.listStudentsAssignment = listStudentsAssignment;
        this.saveHandler = saveHandler;
        this.deleteHandler = deleteHandler;
        this.assignmentController = assignmentController;
        this.assignmentLoader = assignmentLoader;

        ObservableList<Student> observableStudentList = FXCollections.observableList(supplier.get());
        filteredStudents = new FilteredList<>(observableStudentList);

        this.studentsAssignment = new StudentsAssignment();
        List<Student> studentList = listStudentsAssignment.stream() //All student.getStudentId come with Id 0
                .map(StudentsAssignment::getStudent)
                .collect(Collectors.toList());
        this.studentIdList = listStudentsAssignment.stream()
                .map(StudentsAssignment::getStudentId)
                .collect(Collectors.toList());
        List<String> studentNameList = studentList.stream()
                .map(Student::getName)
                .collect(Collectors.toList());

        // 2. Initialize elements content in the scene
        title.setText("Assign Student for:\n"+assignment.getName());

        studentNameLabel.setUserData("mainStudent"); //In room 'Ppal'
        assistantNameLabel.setUserData("assistantStudent"); //In room 'Ppal'

        studentNameLabel.setId(null); //In room 'Ppal'
        assistantNameLabel.setId(null); //In room 'Ppal'

        for (Object nodo : studentsGridPane.getChildren()) {

            if (nodo instanceof Label) {
                Label label = (Label) nodo;
                for(int i=0;i<listStudentsAssignment.size();i++) {
                    StudentsAssignment studentsAssignment = listStudentsAssignment.get(i);
                    Student student = listStudentsAssignment.get(i).getStudent();
                    if (Objects.equals(studentsAssignment.getRolStudent(), label.getUserData())) {
                        label.setId(studentsAssignment.getStudentId() + "");
                        label.setText(student.getName() + " " + student.getLastName());
                    }
                }
            }

        }
        //// Initial Predicate to filteredStudents according to students in ListAssignment content
        Predicate<Student> initialFilter = student -> {
            return !studentIdList.contains(student.getStudentId());
        };
        filteredStudents.setPredicate(initialFilter);
        studentForAssignmentTable.setItems(filteredStudents);
        //If len listStudentsAssignment > 3 (only consider Ppal room), SAVE METHOD shouldn't be allowed

        // 3. listeners
        //// From @FXML initialize(). Start
        rolSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                // Update label text with actual slider value
                sliderValueLabel.setText("Slider Value: " + newValue.intValue());
            }
        });
        //// From @FXML initialize(). End
        //// Add ChangeListener at labels in order to filter studentsForAssignmentTable according to id(s) in labels
        //TODO: for label in GridPane
        studentNameLabel.idProperty().addListener((observable, oldValue, newValue) -> {
            // Update candidatesIdsToDeleteList
            if(!flagCandidatesToDeleteFromStudent) {
                if (oldValue != null && !oldValue.equals("null")) {
                    try {
                        candidatesIdsToDeleteList.add(Integer.valueOf(oldValue));
                    } catch (NumberFormatException e) {
                        System.err.println("Convert to Integer Error: " + e.getMessage());
                    }
                }
                flagCandidatesToDeleteFromStudent = true;
            }
            // Update FilteredList Predicate at change label id: WITH directly setPredicate //This lost effect when applyFilters()
            String assistantNameLabelId = assistantNameLabel.getId();
            filteredStudents.setPredicate(item -> {
                String itemID = Integer.toString(item.getStudentId());
                if (oldValue != null && !oldValue.equals("null")) {
                    return  !itemID.equals(newValue) ||
                            itemID.equals(oldValue); /*||
                            !itemID.equals(assistantNameLabelId);*/
                }
                else {
                    return !itemID.equals(newValue) ;
                }
            });
            //applyFilters(); //NO!
            //studentForAssignmentTable.setItems(filteredStudents); //This isn't necessary
        });

        assistantNameLabel.idProperty().addListener((observable, oldValue, newValue) -> {
            if(!flagCandidatesToDeleteFromAssistant) {
                if (oldValue != null && !oldValue.equals("null")) {
                    try {
                        candidatesIdsToDeleteList.add(Integer.valueOf(oldValue));
                    } catch (NumberFormatException e) {
                        System.err.println("Convert to Integer Errorr: " + e.getMessage());
                    }
                }
                flagCandidatesToDeleteFromAssistant = true;
            }
            // Update FilteredList Predicate at change label id: WITH Predicates //This lost effect when applyFilters()
            String studentNameLabelId = studentNameLabel.getId();
            Predicate<Student> condition1 = item -> {
                String itemID = Integer.toString(item.getStudentId());
                return !itemID.equals(newValue);
            };
            Predicate<Student> condition2 = item -> {
                String itemID = Integer.toString(item.getStudentId());
                return itemID.equals(oldValue);
            };
            Predicate<Student> condition3 = item -> {
                String itemID = Integer.toString(item.getStudentId());
                return !itemID.equals(studentNameLabelId);
            };
            filteredStudents.setPredicate(condition1.or(condition2)); //.or(condition3)
            //applyFilters(); //NO! BETTER store Predicates(?)
            //studentForAssignmentTable.setItems(filteredStudents);
        });

        ////filtered for search input
        // 1. Set the filter Predicate whenever the filter changes.
        name.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredStudents.setPredicate(student -> {
                // If filter text is empty, display all persons.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                // Compare first name and last name field in your object with filter.
                String lowerCaseFilter = newValue.toLowerCase();
                if (String.valueOf(student.getName()).toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                    // Filter matches first name.
                } else if (String.valueOf(student.getLastName()).toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches last name.
                }
                return false; // Does not match.
            });
        });
        // 2. Wrap the FilteredList in a SortedList.
        SortedList sortedData = new SortedList<>(filteredStudents);
        // 3. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(studentForAssignmentTable.comparatorProperty());
        // 4. Add sorted (and filtered) data to the table.
        studentForAssignmentTable.setItems(sortedData);

        // 4. For Drag & Drop Actions
        //// FROM @FXML initialize(). Start
        studentNameLabel.setOnDragOver(this::handleDragOver);
        studentNameLabel.setOnDragDropped(this::handleDragDropped);
        assistantNameLabel.setOnDragOver(this::handleDragOver);
        assistantNameLabel.setOnDragDropped(this::handleDragDropped);
        //// From @FXML initialize(). End

    }

    @FXML
    public void initialize() {

    }

    @FXML void deleteFromLabel1() {
        studentNameLabel.setId(null);
        studentNameLabel.setText("");
    }

    @FXML void deleteFromLabel2() {
        assistantNameLabel.setId(null);
        assistantNameLabel.setText("");
    }

    @FXML
    public void applyFilters() {

        updateTableView();

        RadioButton selectedRadioButton = (RadioButton) genderToggleGroup.getSelectedToggle();
        final String selectedGender;

        if (selectedRadioButton != null) {
            selectedGender = selectedRadioButton.getText();
        } else {
            selectedGender = null;
        }

        filteredStudents.setPredicate(student -> {

            boolean genderCondition = true;
            boolean rolCondition = true;

            // Filter by gender (genre)
            if (selectedGender != null) {
                genderCondition = selectedGender.equals(student.getGenre());
            }

            // Filter by rol // TODO: similar filter by age
            int selectedRol = (int) rolSlider.getValue();
            if(selectedRol != 0 ) {
                rolCondition = student.getRolId() == selectedRol;
            }

            // Mix two conditions using AND
            return genderCondition && rolCondition;

        });

    }

    @FXML
    public void clearFilters() {
        // Deselect all Radiobuttons
        if(null != genderToggleGroup.getSelectedToggle()) {
            genderToggleGroup.getSelectedToggle().setSelected(false);
        }

        // Restart rol filter
        rolSlider.setValue(0);

        applyFilters();
        updateTableView();
    }

    private void updateTableView() {
        // Reload data from original source or only refresh table
        studentForAssignmentTable.refresh();
    }

    @FXML
    private void handleDragDetected(javafx.scene.input.MouseEvent event) {

        Dragboard dragboard = studentForAssignmentTable.startDragAndDrop(TransferMode.COPY);
        ClipboardContent content = new ClipboardContent();

        Student studentItem = studentForAssignmentTable.getSelectionModel().getSelectedItem();
        content.putString(studentItem.getName() + " " + studentItem.getLastName());
        content.put(DataItem.DATA_FORMAT, Integer.toString(studentItem.getStudentId()));

        draggedStudentId = studentItem.getStudentId();

        dragboard.setContent(content);

        event.consume();

    }

    @FXML
    private void handleDragOver(javafx.scene.input.DragEvent event) {

        if (event.getGestureSource() != event.getSource() && event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.COPY);
        }

        event.consume();

    }

    @FXML
    private void handleDragDropped(javafx.scene.input.DragEvent event) {

        Label targetLabel = (Label) event.getGestureTarget();

        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasString()) {
            targetLabel.setText(db.getString());
            targetLabel.setId(draggedStudentId+"");
            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void save() {

        HashMap<Integer, String> hashMapLabels = new HashMap<>();
        for (Object nodo : studentsGridPane.getChildren()) {
            if (nodo instanceof Label) {
                Label label = (Label) nodo;

                // Get labelId (fx:id), which is studentId
                String idString = label.getId();
                if (idString != null && !idString.isEmpty()) {
                    int id = Integer.parseInt(idString);

                    // Get content getUserData, which is "mainStudent" or "assistantStudent" //in "Ppal" room
                    Object userData = label.getUserData();
                    if (userData instanceof String) {
                        String contenido = (String) userData;

                        // Add to HashMap
                        hashMapLabels.put(id, contenido);
                    }
                }
            }
        }

        List<Integer> labelIds = new ArrayList<>(hashMapLabels.keySet());

        System.out.println("Previous in label (existing in db)" + studentIdList);
        System.out.println("Candidates to Save" + labelIds);
        System.out.println("Candidates to Delete" + candidatesIdsToDeleteList);

        List<Integer> toSaveIds = FormatUtils.differenceLists(labelIds,
                FormatUtils.joinLists(studentIdList, candidatesIdsToDeleteList)); //to save
        List <Integer> toDeleteIds = FormatUtils.intersectLists(candidatesIdsToDeleteList,
                studentIdList); //to delete

        System.out.println("to Save " + toSaveIds);
        System.out.println("to Delete " + toDeleteIds);

        Dialog.DialogBuilder.builder()
                .title("Save Changes")
                .message(String.format("Do you want to save changes for Assignment %s?", assignment.getName()))
                .okActionListener(() -> {

                    for(int id:toSaveIds){
                        StudentsAssignment studentsAssignment =
                                new StudentsAssignment(assignment.getAssignmentId(),
                                        id,
                                        hashMapLabels.get(id),
                                        "Ppal"); //TODO:SELECT room FROM Combobox
                        saveHandler.accept(studentsAssignment);
                    }

                    for(int id:toDeleteIds){ // TODO: in one query: WHERE ... OR ...
                        deleteHandler.accept(Integer.toString(id),
                                Integer.toString(assignment.getAssignmentId()));
                    }

                    close();

                })
                .build().show();
                //loadView(Menu.Assignment);

    }

    @FXML
    private void close() throws IOException {

        //initialize();

        //MainFrame.show();

        //Assignments controller = Assignments.getController();

        // Assignments.show(title);

        /*Stage stage = (Stage) title.getScene().getWindow();
        Parent root1 = assignmentLoader.getRoot();
        stage.setScene(new Scene(root1));
        stage.show();*/

        Assignments assignmentController = assignmentLoader.getController(); //Null!!
        title.getScene().getWindow().hide();
        //assignmentController.close();

        //title.getScene().getWindow().hide();
        //MainFrame.show();

    }

    @FXML
    public void search(ActionEvent actionEvent) {
        filteredStudents.setPredicate(student -> {
            return true;
        });
    }

}