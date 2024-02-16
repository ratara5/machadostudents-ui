package org.machado.machadostudentsui.views;


import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.machado.machadostudentsclient.WebClientMachado;
import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;
import org.machado.machadostudentsui.utils.PdfAssignmentManager;
import org.machado.machadostudentsui.views.common.Dialog;
import org.machado.machadostudentsui.views.popups.AssignmentEdit;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.kernel.colors.WebColors;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.property.TextAlignment;

@Controller
public class Assignments
        extends AbstractController
        implements Consumer<List<Assignment>>, Supplier<List<Student>>{

    @FXML
    DatePicker datePickerStart;
    @FXML
    DatePicker datePickerEnd;
    @FXML
    private TextField name;
    @FXML
    private TableView<Assignment> assignmentTable;
    @FXML
    private VBox addButton;
    @FXML
    private VBox genPdfButton;
    @FXML
    private VBox genAssButton;



    private Assignments controller;
    @Autowired
    private WebClientMachado webClientMachado;


    @FXML
    public void initialize() throws IOException { // throws IOException

        FXMLLoader loader = new FXMLLoader(Assignments.class.getResource("Assignment.fxml"));
        Assignments controller = (Assignments) loader.getController();

        assignmentTable.getItems().clear(); // Implements Consumer
        webClientMachado.assignmentsAll().subscribe(this); //Implement Consumer

        MenuItem edit = new MenuItem("Assign Student");
        edit.setOnAction(event -> {
            Assignment assignment = assignmentTable.getSelectionModel().getSelectedItem();
            if(null != assignment) {
                AssignmentEdit.edit(assignment,
                        (Consumer<StudentsAssignment>) this::save,
                        (BiConsumer<String, String>) this::getDeleteStudentAssignmentConsumer,
                        (List<StudentsAssignment>) this.getStudentsAssignmentByAssignment(assignment.getAssignmentId()),
                        (Supplier<List<Student>>) this::get,
                        (Assignments) controller,
                        (FXMLLoader) loader);
            }
        });

        assignmentTable.setContextMenu(new ContextMenu(edit));

        Tooltip tooltip1 = new Tooltip("Add new Assignment");
        tooltip1.setFont(new Font("Arial", 16));
        Tooltip.install(addButton, tooltip1);

        Tooltip tooltip2 = new Tooltip("Generate Pdf Program");
        tooltip2.setFont(new Font("Arial", 16));
        Tooltip.install(genPdfButton, tooltip2);

        Tooltip tooltip3 = new Tooltip("Generate Pdf Assignments");
        tooltip3.setFont(new Font("Arial", 16));
        Tooltip.install(genAssButton, tooltip3);

    }

    @FXML
    private void search() {
        assignmentTable.getItems().clear();
        String dateStartString = datePickerStart.getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dateEndString = datePickerEnd.getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<Assignment> assignmentsBetweenDates = webClientMachado.assignmentsBetweenDates(dateStartString, dateEndString).block();
        assignmentTable.getItems().addAll(assignmentsBetweenDates);
    }

    @FXML
    private void clear() {
        datePickerStart.setValue(null);
        datePickerEnd.setValue(null);
        name.clear();
        assignmentTable.getItems().clear();
    }

    //Assignments will be loaded in Server when SpringbootApplication will be running
    @FXML
    private void addNew() {
    }

    private void save(StudentsAssignment studentsAssignment) {
        webClientMachado.addStudentAssignment(studentsAssignment);
        //initialize();
    }

    public void getDeleteStudentAssignmentConsumer(String studentId, String assignmentId) {
        webClientMachado.deleteStudentAssignment(studentId, assignmentId).subscribe();
    }

    private List<StudentsAssignment> getStudentsAssignmentByAssignment(int assignmentId) {
        List<StudentsAssignment> listStudentsAssignment = webClientMachado.studentsPerAssignment(assignmentId+"").block();
        //initialize();
        return listStudentsAssignment;
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

    // BEFORE: THis method in this class
    /*private static Map<LocalDate, Map<String, List<Assignment>>> groupData(List<Assignment> assignments) {

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

    }*/

    /*private static void generatePDF(Map<LocalDate, Map<String, List<Assignment>>> groupedData) {

        float FONT_SIZE = 8f;
        float INTERLINE = 6f;

        try (PdfWriter pdfWriter = new PdfWriter("../machadostudents-ui/output.pdf");
             PdfDocument pdfDoc = new PdfDocument(pdfWriter);
             Document document = new Document(pdfDoc)) {

            document.setFontSize(FONT_SIZE);

            // Iterate into dates
            for (LocalDate date : groupedData.keySet()) {
                Paragraph p=new Paragraph("Fecha: " + date);
                p.setFontSize(12f);
                p.setTextAlignment(TextAlignment.CENTER);
                document.add(p);

                // Iterarate into sections inside each date
                int i=0;
                for (String section : groupedData.get(date).keySet()) {
                    Paragraph pS = new Paragraph("Sección: " + section);
                    switch(i){
                        case 0:
                            pS.setFontColor(WebColors.getRGBColor("86BFCA"),1);
                            break;
                        case 1:
                            pS.setFontColor(WebColors.getRGBColor("#BA9552"),1);
                            break;
                        case 2:
                            pS.setFontColor(WebColors.getRGBColor("#D27674"),1);
                            break;
                    }
                    document.add(pS);
                    i++;

                    // Create table
                    Table table = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}));
                    //table.setBorder(Border.NO_BORDER);

                    // Setup columns width
                    table.setWidth(UnitValue.createPercentValue(100));

                    // Setup headers columns
                    String[] encs = {"Intervención", "Encargado", "Ayudante"};
                    for (String e:encs) {
                        Cell cell = new Cell();
                        //cell.setBorder(Border.NO_BORDER);
                        cell.setBackgroundColor(WebColors.getRGBColor("black"));
                        Paragraph pI = new Paragraph(e);
                        pI.setFontColor(WebColors.getRGBColor("white"),0);
                        pI.setBold();
                        cell.add(pI);
                        table.addCell(cell);
                    }

                    // Iterate upon records inside each date and section
                    for (Assignment assignment : groupedData.get(date).get(section)) {
                        // Add row to table
                        table.addCell(assignment.getName());
                        if (null != assignment.getMainStudentName() && !assignment.getMainStudentName().isEmpty()) {
                            table.addCell(assignment.getMainStudentName()); //table.addCell("Encargado: " + String.join(", ", assignment.getMainStudentName())); //substring(0,15)
                        } else {
                            table.addCell("Pendiente");
                        }
                        if (null != assignment.getAssistantStudentName() && !assignment.getMainStudentName().isEmpty()) {
                            table.addCell(assignment.getAssistantStudentName());
                        } else {
                            table.addCell("                    "); //table.addCell("No Requerido o Pendiente");
                        }
                    }

                    // Add table to document
                    document.add(table);

                }
                // Separation between dates
                document.add(new Paragraph().setFixedLeading(INTERLINE));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/

    @FXML
    private void generatePdf() {

        if(datePickerStart.getValue() == null || datePickerEnd.getValue() == null){

            Dialog.DialogBuilder.builder()
                    .title("Select period")
                    .message(String.format("Please, select start and end dates"))
                    .build().show();

        } else {

            String dateStartString = datePickerStart.getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String dateEndString = datePickerEnd.getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            List<Assignment> assignments = webClientMachado.assignmentsBetweenDates(dateStartString, dateEndString).block();

            Map<LocalDate, Map<String, List<Assignment>>> groupedData = PdfAssignmentManager.groupData(assignments);
            // Generate PDF
            /*generatePDF(groupedData);*/ //BEFORE with generatePDF method in this class
            PdfAssignmentManager.generatePDF(groupedData);


        }

    }

    // BEFORE: THis method in this class
    /*public  void fillForms(List<Assignment> assignments) { //public static void

        try {
            int i = 0;
            for (Assignment assignment : assignments) {
                // Path to template form: Made manually
                String templateForm = "../machadostudents-ui/template/FormatoAsignacionVMC.pdf";

                // Assignments without students will not be generated
                if(assignment.getMainStudentName() != null) {

                    List<StudentsAssignment> listStudentsAssignment = getStudentsAssignmentByAssignment(assignment.getAssignmentId());
                    Optional<Integer> mainStudentId = listStudentsAssignment.stream()
                            .filter(student -> "mainStudent".equals(student.getRolStudent()))
                            .map(StudentsAssignment::getStudentId)
                            .findFirst();
                    // Ever present, as "mainStudent" is the DEFAULT value in rol_student
                        Student mainStudent = getStudentById(mainStudentId.orElse(0));
                        String nameMainStudent = mainStudent.getName();

                    // Output path filled form
                    String outputPath = "../machadostudents-ui/assignments/"
                            + nameMainStudent.replaceAll("\\s", "")
                            + String.valueOf(i)
                            + ".pdf";

                    // Create a new document template based
                    PdfReader reader = new PdfReader(templateForm);
                    PdfWriter writer = new PdfWriter(outputPath);
                    PdfDocument pdfDocument = new PdfDocument(reader, writer);
                    PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDocument, true);

                    float FONT_SIZE = 5f;

                    // Fill fields
                    form.getField("900_1_Text_SanSerif").setValue(nameMainStudent).setFontSize(FONT_SIZE);
                    //// Fill assistant correctly in form
                    Optional<Integer> assistantStudentId = listStudentsAssignment.stream()
                            .filter(student -> "assistantStudent".equals(student.getRolStudent()))
                            .map(StudentsAssignment::getStudentId)
                            .findFirst();
                    if(assistantStudentId.isPresent()) {
                        Student assistantStudent = getStudentById(assistantStudentId.orElse(0));
                        String nameAssistantStudent = assistantStudent.getName();
                        form.getField("900_2_Text_SanSerif").setValue(nameAssistantStudent).setFontSize(FONT_SIZE);
                    } else {
                        String nameAssistantStudent = "";
                        form.getField("900_2_Text_SanSerif").setValue(nameAssistantStudent).setFontSize(FONT_SIZE);
                    }
                    //// Fill other fields in form
                    form.getField("900_3_Text_SanSerif").setValue(assignment.getDate().toString()).setFontSize(FONT_SIZE);
                    form.getField("900_4_Text_SanSerif").setValue(assignment.getName()).setFontSize(FONT_SIZE);
                    form.getField("900_5_CheckBox")
                            .setCheckType(PdfFormField.TYPE_CHECK)
                            .setValue("Yes")
                            .setBackgroundColor(new DeviceRgb(0, 255, 0)); // Yet, isn't possible modify any checkbox property

                    // Close PDF document
                    pdfDocument.close();

                    // In DialogBox
                    System.out.println("Formulario para " + assignment.getName() + " generado en: " + outputPath);

                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/

    @FXML
    private void generateAssignments() {

        if(datePickerStart.getValue() == null || datePickerEnd.getValue() == null){

            Dialog.DialogBuilder.builder()
                    .title("Select period")
                    .message(String.format("Please, select start and end dates"))
                    .build().show();

        } else {

            String dateStartString = datePickerStart.getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String dateEndString = datePickerEnd.getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            List<Assignment> assignments = webClientMachado.assignmentsBetweenDates(dateStartString, dateEndString).block();
            /*fillForms(assignments);*/ //BEFORE with fillForms method in this class
            List<StudentsAssignment> listStudentsAssignment = webClientMachado.studentsAssignmentAll().block();
            PdfAssignmentManager.fillForms(assignments, listStudentsAssignment);

        }

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