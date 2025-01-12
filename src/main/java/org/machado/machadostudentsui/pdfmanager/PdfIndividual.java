package org.machado.machadostudentsui.pdfmanager;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import jakarta.annotation.PostConstruct;
import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;
import org.machado.machadostudentsui.utils.FormatUtils;
import org.machado.machadostudentsui.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PdfIndividual {

    @Value("${outputAssignments.path}")
    String outputAssignmentsPath;


    public String getTemplateFormPath() throws IOException {
        // Obtiene el archivo como un flujo de entrada desde el classpath
        InputStream inputStream = PdfIndividual.class.getClassLoader().getResourceAsStream("templates/FormatoAsignacionVMC.pdf");

        if (inputStream == null) {
            throw new IOException("El archivo no se encuentra en el classpath");
        }

        // Crea un archivo temporal en el sistema
        File tempFile = File.createTempFile("FormatoAsignacionVMC" + System.nanoTime(), ".pdf");
        tempFile.deleteOnExit();  // Asegura que se elimine cuando termine el programa

        // Copia el archivo desde el InputStream al archivo temporal
        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Retorna la ruta absoluta del archivo temporal
        return tempFile.getAbsolutePath();
    }



    public File getOutputFolder() { //accede al campo scriptPath sin necesidad de pasarla como parámetro, ya que es un campo de instancia de la clase, pues es inyectado por Spring con la anotación @Value
        String basePath = System.getProperty("user.dir"); // Directorio de trabajo actual
        System.out.println("outputAssignmentsPath es: " + outputAssignmentsPath);
        return new File(basePath, outputAssignmentsPath);  // Combina con la ruta relativa // File asegura el separador correcto
    }



    public String getOutputFileName (String outputAssignmentsFlexPath, String phoneNumber, int i) {
        String name = phoneNumber.replaceAll("\\s+", "")
                + "-"
                + String.valueOf(i)
                + ".pdf";
        return outputAssignmentsFlexPath + File.separator + name;
    }


    public void fillFieldsForm(int i, Assignment assignment, List<StudentsAssignment> listStudentAssignmentRoom, String room) throws IOException {
        // Path to template form: Made manually
        //String templateForm = "../machadostudents-ui/template/FormatoAsignacionVMC.pdf";
        String templateForm = getTemplateFormPath(); //PdfIndividual.class.getClassLoader().getResource("templates/FormatoAsignacionVMC.pdf").toString();

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
        File outputFolder = getOutputFolder();
        // + "/output/assignments/";
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        String outputAssignmentsFlexPath = outputFolder.getAbsolutePath();

        // Output path filled form
        //String outputPath = "../machadostudents-ui/assignments/"
        String outputAssignmentsFlexFullPath = getOutputFileName(outputAssignmentsFlexPath, phoneMainStudent, i);

        // Create a new document template based
        PdfReader reader = new PdfReader(templateForm);
        PdfWriter writer = new PdfWriter(outputAssignmentsFlexFullPath);
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
        System.out.println("Formulario para " + assignment.getName() + " generado en: " + outputAssignmentsFlexFullPath);

    }



    public void fillForms(List<Assignment> assignments, List<StudentsAssignment> listStudentsAssignment) { //public static void

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