package org.machado.machadostudentsui.utils;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.WebColors;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PdfAssignmentManager {

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

    public static void generatePDF(Map<LocalDate, Map<String, List<Assignment>>> groupedData) {

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
                        case 1: //0. Because 0 and four are now PRESIDENCIA Y ORACION FINAL
                            pS.setFontColor(WebColors.getRGBColor("86BFCA"),1);
                            break;
                        case 2: //1. Because 0 and four are now PRESIDENCIA Y ORACION FINAL
                            pS.setFontColor(WebColors.getRGBColor("#BA9552"),1);
                            break;
                        case 3: //2. Because 0 and four are now PRESIDENCIA Y ORACION FINAL
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

    }

    public static void fillForms(List<Assignment> assignments, List<StudentsAssignment> listStudentsAssignment) { //public static void

        try {
            int i = 0;
            for (Assignment assignment : assignments) {
                // Path to template form: Made manually
                String templateForm = "../machadostudents-ui/template/FormatoAsignacionVMC.pdf";

                // Assignments without students will not be generated
                if(assignment.getMainStudentName() != null) {

                    //filteredListStudentsAssignment contains two elements max
                    List<StudentsAssignment> filteredListStudentsAssignment = listStudentsAssignment
                            .stream()
                            .filter(x->x.getAssignmentId() == assignment.getAssignmentId())
                            .collect(Collectors.toList());

                    Optional<Student> mainStudent = filteredListStudentsAssignment.stream()
                            .filter(student -> "mainStudent".equals(student.getRolStudent()))
                            .map(StudentsAssignment::getStudent)
                            .findFirst();

                    // Ever present, as "mainStudent" is the DEFAULT value in rol_student
                    String nameMainStudent = mainStudent.get().getName();

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
                    Optional<Student> assistantStudent = filteredListStudentsAssignment.stream()
                            .filter(student -> "assistantStudent".equals(student.getRolStudent()))
                            .map(StudentsAssignment::getStudent)
                            .findFirst();
                    if(assistantStudent.isPresent()) {
                        //Student assistantStudent = getStudentById(assistantStudentId.orElse(0));
                        String nameAssistantStudent = assistantStudent.get().getName();
                        form.getField("900_2_Text_SanSerif").setValue(nameAssistantStudent).setFontSize(FONT_SIZE);
                    } else {
                        String nameAssistantStudent = "";
                        form.getField("900_2_Text_SanSerif").setValue(nameAssistantStudent).setFontSize(FONT_SIZE);
                    }
                    //// Fill other fields in form
                    //form.getField("900_3_Text_SanSerif").setValue(assignment.getDate().toString()).setFontSize(FONT_SIZE);
                    String yearNumber = assignment.getDate().toString().substring(0,4);
                    String monthNumber = assignment.getDate().toString().substring(5,7);
                    String dayNumber = assignment.getDate().toString().substring(8,10);

                    String monthName = FormatUtils.monthOfNumber.getOrDefault(monthNumber, "Mes").substring(0,3);
                    form.getField("900_3_Text_SanSerif").setValue( dayNumber + " de " + monthName + " de " + yearNumber).setFontSize(FONT_SIZE);
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

    }

}
