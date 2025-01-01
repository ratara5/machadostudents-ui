package org.machado.machadostudentsui.utils;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.WebColors;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;
import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.machado.machadostudentsui.utils.FormatUtils.meses;

public class PdfAssignmentManager {

    private static Map<Integer, Map<String, String>> styleMap = new HashMap<>();

    /**
     * Method: convert color from hexadecimal to RGB integers array.
     * @param hexColor Color in hexadecimal format (with or without '#').
     * @return RGB Integers array.
     */
    public static int[] hexToRgb(String hexColor) {
        if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        }
        int red = Integer.valueOf(hexColor.substring(0, 2), 16);
        int green = Integer.valueOf(hexColor.substring(2, 4), 16);
        int blue = Integer.valueOf(hexColor.substring(4, 6), 16);
        return new int[]{red, green, blue};
    }

    /**
     * *Method: convert from integers array string to integers array.
     * @param rgbString String in "[r, g, b]" format.
     * @return Integers array corresponding to RGB components.
     */
    public static int[] stringToRgbArray(String rgbString) {
        rgbString = rgbString.replaceAll("\\[|\\]|\\s", "");  // Delete square braces and spaces
        String[] rgbStrArray = rgbString.split(",");          // Separation by comma
        int[] rgbArray = new int[rgbStrArray.length];
        for (int i = 0; i < rgbStrArray.length; i++) {
            rgbArray[i] = Integer.parseInt(rgbStrArray[i]);   // To integer conversion
        }
        return rgbArray;
    }

    public static void getColorsMap() {
        // styleMap es el mapa principal que contendrá los estilos para diferentes valores de i


        // Styles for i=0 y i=4
        Map<String, String> style0 = new HashMap<>();
        style0.put("fontColor", "#000000"); // Black
        style0.put("bgColor", "#90B1DD"); // Light blue
        styleMap.put(0, style0);
        styleMap.put(4, style0); // Same style for i=4

        // Styles for i=1
        Map<String, String> style1 = new HashMap<>();
        style1.put("fontColor", "#7ABCC6"); // Extra light turquoise
        style1.put("bgColor", "#2B6C75"); // Turquoise
        styleMap.put(1, style1);

        // Styles for i=2
        Map<String, String> style2 = new HashMap<>();
        style2.put("fontColor", "#FFCA64"); // Extra light mustard
        style2.put("bgColor", "#936924"); // Dark mustard
        styleMap.put(2, style2);

        // Styles for i=3
        Map<String, String> style3 = new HashMap<>();
        style3.put("fontColor", "#CD7473"); // Extra light terracotta
        style3.put("bgColor", "91312D"); // Terracotta
        styleMap.put(3, style3);

        // Add "borderColor" key with RGB values corresponding to "bgColor"
        for (Map.Entry<Integer, Map<String, String>> entry : styleMap.entrySet()) {
            Map<String, String> styles = entry.getValue();
            String bgColor = styles.get("bgColor");
            if (bgColor != null) {
                int[] rgb = hexToRgb(bgColor);
                styles.put("borderColor", Arrays.toString(rgb));
            }
        }
    }

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

    public static <K, V> K getFirstKey(Map<K, V> map) {
        // Get iterator from key set
        Iterator<K> iterator = map.keySet().iterator();

        // Verify if there is a first key and return it
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            return null; //Empty map
        }
    }

    public static void addStudentCell(Cell cell, Optional<Student> mainStudent, Optional<Student> assistantStudent, int i, Map<Integer, Map<String, String>> styleMap, Table table) {
        String borderColorStr = styleMap.get(i).get("borderColor");
        int[] borderColorRgb = stringToRgbArray(borderColorStr);
        DeviceRgb borderColor = new DeviceRgb(borderColorRgb[0], borderColorRgb[1], borderColorRgb[2]);
        SolidBorder border = new SolidBorder(borderColor, 1);

        // Set background color if applicable
        if (i == 0 || i == 4) {
            cell.setBackgroundColor(WebColors.getRGBColor(styleMap.get(i).get("bgColor")));
        }

        // Add main student name
        String mainFullName = mainStudent
                .map(s -> String.format("%s %s", s.getName(), s.getLastName()))
                .orElse("                  ");
        Paragraph pN = new Paragraph(mainFullName);
        pN.setCharacterSpacing(0.4f);
        cell.add(pN);

        // Add assistant student name
        String assistantFullName = assistantStudent
                .map(s -> String.format("%s %s", s.getName(), s.getLastName()))
                .orElse("                  ");
        Paragraph pH = new Paragraph(assistantFullName);
        pH.setCharacterSpacing(0.4f);
        cell.add(pH);

        cell.setBorder(border);
        table.addCell(cell);
    }


    public static void generatePDF(Map<LocalDate, Map<String, List<Assignment>>> groupedData, List<StudentsAssignment> listStudentsAssignment) {

        float FONT_SIZE = 8f;
        float INTERLINE = 6f;

        getColorsMap();

        try (PdfWriter pdfWriter = new PdfWriter("../machadostudents-ui/output.pdf");
             PdfDocument pdfDoc = new PdfDocument(pdfWriter);
             Document document = new Document(pdfDoc)) {

            document.setCharacterSpacing(0.6f);

            LocalDate firstKey = getFirstKey(groupedData);
            String primerMes = firstKey.getMonth().toString();
            String mes = primerMes.substring(0, 1).toUpperCase() + primerMes.substring(1).toLowerCase();

            Paragraph pHead=new Paragraph("VIDA & MINISTERIO CRISTIANOS - " + meses.getOrDefault(mes, "Mes no encontrado").toUpperCase() + " 2025");
            pHead.setFontSize(14f);
            pHead.setTextAlignment(TextAlignment.CENTER);
            pHead.setBold();
            document.add(pHead);

            document.setFontSize(FONT_SIZE);

            // Iterate into dates
            outerLoop:
            for (LocalDate date : groupedData.keySet()) {

                Class<?> myAssignment = groupedData.get(date).get("TESOROS DE LA BIBLIA").get(0).getClass();
                java.lang.reflect.Method readingMethod = myAssignment.getMethod("getReading");
                Object returnedObject = readingMethod.invoke(groupedData.get(date).get("TESOROS DE LA BIBLIA").get(0));
                String reading = (String) returnedObject;


                Paragraph p=new Paragraph("MIÉRCOLES " + date.getDayOfMonth() + " / " + reading);
                p.setFontSize(11f);
                p.setFontColor(WebColors.getRGBColor("#5A5D5E"),1);
                p.setBold();
                p.setTextAlignment(TextAlignment.LEFT);
                document.add(p);

                // Iterate through sections inside each date
                int i=0;
                for (String section : groupedData.get(date).keySet()) {

                    // Create table
                    float[] columnWidths = {50, 25, 25};
                    Table table = new Table(UnitValue.createPercentArray(columnWidths));
                    //table.setBorder(Border.NO_BORDER);

                    // Setup columns width
                    table.setWidth(UnitValue.createPercentValue(100));

                    // Section name
                    Paragraph pS = new Paragraph(section);
                    String borderColorStr = styleMap.get(i).get("borderColor");
                    int[] borderColorRgb = stringToRgbArray(borderColorStr);
                    DeviceRgb borderColor = new DeviceRgb(borderColorRgb[0], borderColorRgb[1], borderColorRgb[2]);
                    SolidBorder border = new SolidBorder(borderColor, 1);

                    // Cell with section name. Excepts "PRESIDENCIA" & "ORACIÓN FINAL"
                    if(i != 0 && i != 4) {
                        Cell cell1 = new Cell(1, 3);

                        cell1.setBackgroundColor(WebColors.getRGBColor(styleMap.get(i).get("bgColor")));
                        pS.setFontColor(WebColors.getRGBColor(styleMap.get(i).get("fontColor")));
                        pS.setBold();

                        cell1.add(pS);
                        table.addCell(cell1);
                        table.setBorder(border);
                    }

                    // Iterate records inside each date and section
                    for (Assignment assignment : groupedData.get(date).get(section)) {

                        if (assignment.isWeekWithoutMeet()) {
                            Cell cellWithoutMeet = new Cell(1, 3);

                            cellWithoutMeet.add(new Paragraph("Semana sin reunión"));
                            table.addCell(cellWithoutMeet);

                            document.add(table);
                            continue outerLoop;
                        }

                        // Retrieve studentsAssignments in Ppal and Aux rooms
                        Map<String, List<StudentsAssignment>> result = getStudentsAssignmentsByRoom(assignment, listStudentsAssignment);
                        List<StudentsAssignment> listStudentAssignmentPpal = result.get("Ppal");
                        List<StudentsAssignment> listStudentAssignmentAux = result.get("Aux");

                        // Retrieve students in Ppal room
                        Optional<Student> mainStudentPpal = getStudentInAssignment(listStudentAssignmentPpal, "mainStudent");
                        Optional<Student> assistantStudentPpal = getStudentInAssignment(listStudentAssignmentPpal, "assistantStudent");

                        // Retrieve students in Aux room
                        Optional<Student> mainStudentAux = getStudentInAssignment(listStudentAssignmentAux, "mainStudent");
                        Optional<Student> assistantStudentAux = getStudentInAssignment(listStudentAssignmentAux, "assistantStudent");


                        // Add row to table
                        Cell cellAssignmentName = new Cell();

                        //trim assignment name if its width is greater than respective cell width
                        PdfFont font = PdfFontFactory.createFont();
                        String assignmentNameString = assignment.getName();

                        float tableWidth = pdfDoc.getDefaultPageSize().getWidth() - document.getLeftMargin() - document.getRightMargin();
                        float cellAssignmentNameWidth = tableWidth * columnWidths[0] / 100f;

                        float assignmentNameTextWidth = font.getWidth(assignmentNameString, FONT_SIZE);
                        String assignmentName = assignmentNameTextWidth > cellAssignmentNameWidth ?
                                assignment.getName().substring(0, 54) : //54 obtained by means of count characters in cell for assignment name in test pdf
                                assignment.getName();

                        // Create paragraph
                        Paragraph pA = new Paragraph(assignmentName);
                        pA.setBold();

                        if (i != 0 && i != 4) {
                            pA.setFontColor(WebColors.getRGBColor(styleMap.get(i).get("bgColor")));

                        } else {
                            cellAssignmentName.setBackgroundColor(WebColors.getRGBColor(styleMap.get(i).get("bgColor")));
                        }
                        cellAssignmentName.add(pA);
                        cellAssignmentName.setBorder(border);
                        cellAssignmentName.setVerticalAlignment(VerticalAlignment.MIDDLE);

                        table.addCell(cellAssignmentName);

                        // Cells for students and rooms in assignments
                        Cell cellPpalStudents = new Cell();
                        addStudentCell(cellPpalStudents, mainStudentPpal, assistantStudentPpal, i, styleMap, table);
                        Cell cellAuxStudents = new Cell();
                        addStudentCell(cellAuxStudents, mainStudentAux, assistantStudentAux, i, styleMap, table);

                    }

                    // Add table to document
                    document.add(table);

                    i++;

                }

                // Separation between dates
                document.add(new Paragraph().setFixedLeading(INTERLINE));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

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

    public static void fillFieldsForm(int i, Assignment assignment, List<StudentsAssignment> listStudentAssignmentRoom, String room) throws IOException {
        // Path to template form: Made manually
        String templateForm = "../machadostudents-ui/template/FormatoAsignacionVMC.pdf";

        // Retrieve students by room
        Optional<Student> mainStudent = getStudentInAssignment(listStudentAssignmentRoom, "mainStudent");
        Optional<Student> assistantStudent = getStudentInAssignment(listStudentAssignmentRoom, "assistantStudent");

        if (!mainStudent.isPresent()) {
            return;
        }

        // Ever present, as "mainStudent" is the DEFAULT value in rol_student
        String nameMainStudent = mainStudent.get().getName() + " " + mainStudent.get().getLastName() ;
        String phoneMainStudent = mainStudent.get().getPhoneNumber();

        // Output path filled form
        String outputPath = "../machadostudents-ui/assignments/"
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
                    Map<String, List<StudentsAssignment>> result = getStudentsAssignmentsByRoom(assignment, listStudentsAssignment);

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
