package org.machado.machadostudentsui.pdfmanager;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;
import org.machado.machadostudentsui.utils.FormatUtils;
import org.machado.machadostudentsui.utils.SearchUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PdfIndividual {

    public static void fillFieldsForm(int i, Assignment assignment, List<StudentsAssignment> listStudentAssignmentRoom, String room) throws IOException {
        // Path to template form: Made manually
        //String templateForm = "../machadostudents-ui/template/FormatoAsignacionVMC.pdf";
        String templateForm = PdfIndividual.class.getClassLoader().getResource("templates/FormatoAsignacionVMC.pdf").toString();

        // Retrieve students by room
        Optional<Student> mainStudent = SearchUtils.getStudentInAssignment(listStudentAssignmentRoom, "mainStudent");
        Optional<Student> assistantStudent = SearchUtils.getStudentInAssignment(listStudentAssignmentRoom, "assistantStudent");

        if (!mainStudent.isPresent()) {
            return;
        }

        // Ever present, as "mainStudent" is the DEFAULT value in rol_student
        String nameMainStudent = mainStudent.get().getName() + " " + mainStudent.get().getLastName() ;
        String phoneMainStudent = mainStudent.get().getPhoneNumber();

        // Create output directory
        String outputDir = System.getProperty("user.dir") + "/output/assignments/";
        File outputFolder = new File(outputDir);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        // Output path filled form
        //String outputPath = "../machadostudents-ui/assignments/"
        String outputPath = outputDir
                + phoneMainStudent.replaceAll("\\s", "")
                //+ nameMainStudent.replaceAll("\\s", "")
                + "-"
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

        //// Fill assistant name
        String nameAssistantStudent;
        if(assistantStudent.isPresent()) {
            //Student assistantStudent = getStudentById(assistantStudentId.orElse(0));
            nameAssistantStudent = assistantStudent.get().getName() + " " + assistantStudent.get().getLastName();
        } else {
            nameAssistantStudent = "";
        }
        form.getField("900_2_Text_SanSerif").setValue(nameAssistantStudent).setFontSize(FONT_SIZE);
        //// Fill other fields in form
        //form.getField("900_3_Text_SanSerif").setValue(assignment.getDate().toString()).setFontSize(FONT_SIZE);
        String yearNumber = assignment.getDate().toString().substring(0,4);
        String monthNumber = assignment.getDate().toString().substring(5,7);
        String dayNumber = assignment.getDate().toString().substring(8,10);

        String monthName = FormatUtils.monthOfNumber.getOrDefault(monthNumber, "Mes").substring(0,3);
        form.getField("900_3_Text_SanSerif").setValue( dayNumber + " de " + monthName + " de " + yearNumber).setFontSize(FONT_SIZE);
        String assignmentName = assignment.getName(); //.substring(0,20); //0,54
        form.getField("900_4_Text_SanSerif").setValue(assignmentName).setFontSize(FONT_SIZE);

        //// Fill room
        form.getField("900_5_CheckBox").setValue("Off");
        form.getField("900_6_CheckBox").setValue("Off");
        if ("Ppal".equals(room)) {
            form.getField("900_5_CheckBox")

                    .setCheckType(PdfFormField.TYPE_CHECK)
                    .setValue("Yes")
                    .setBackgroundColor(new DeviceRgb(0, 255, 0)); // Yet, isn't possible modify any checkbox property
        } else if ("Aux".equals(room)){
            form.getField("900_6_CheckBox")

                    .setCheckType(PdfFormField.TYPE_CHECK)
                    .setValue("Yes")
                    .setBackgroundColor(new DeviceRgb(0, 255, 0));
        }

        // Close PDF document
        form.flattenFields(); // Permanent changes
        pdfDocument.close();

        // In DialogBox
        System.out.println("Formulario para " + assignment.getName() + " generado en: " + outputPath);

    }

    public static void fillForms(List<Assignment> assignments, List<StudentsAssignment> listStudentsAssignment) { //public static void

        try {
            int i = 0;
            for (Assignment assignment : assignments) {

                // Assignments without students will not be generated
                if(!assignment.isWeekWithoutMeet()) { //&& assignment.getMainStudentName() != null

                    // Retrieve StudentAssignments by room
                    Map<String, List<StudentsAssignment>> result = SearchUtils.getStudentsAssignmentsByRoom(assignment, listStudentsAssignment);

                    List<StudentsAssignment> listStudentAssignmentPpal = result.get("Ppal");
                    fillFieldsForm(i, assignment, listStudentAssignmentPpal, "Ppal");

                    List<StudentsAssignment> listStudentAssignmentAux = result.get("Aux");
                    fillFieldsForm(i, assignment, listStudentAssignmentAux, "Aux");

                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}