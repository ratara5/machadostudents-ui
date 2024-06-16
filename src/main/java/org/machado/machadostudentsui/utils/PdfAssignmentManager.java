package org.machado.machadostudentsui.utils;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.WebColors;
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
import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PdfAssignmentManager {

    /**
     * Método para convertir un color en formato hexadecimal a un array de enteros RGB.
     * @param hexColor Color en formato hexadecimal (con o sin '#').
     * @return Array de enteros correspondiente a los componentes RGB.
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
     * Método para convertir un string de un array de enteros a un array de enteros.
     * @param rgbString String en formato "[r, g, b]".
     * @return Array de enteros correspondiente a los componentes RGB.
     */
    public static int[] stringToRgbArray(String rgbString) {
        rgbString = rgbString.replaceAll("\\[|\\]|\\s", "");  // Eliminar corchetes y espacios
        String[] rgbStrArray = rgbString.split(",");          // Separar por comas
        int[] rgbArray = new int[rgbStrArray.length];
        for (int i = 0; i < rgbStrArray.length; i++) {
            rgbArray[i] = Integer.parseInt(rgbStrArray[i]);   // Convertir a enteros
        }
        return rgbArray;
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

    public static void generatePDF(Map<LocalDate, Map<String, List<Assignment>>> groupedData) {

        float FONT_SIZE = 8f;
        float INTERLINE = 6f;

        try (PdfWriter pdfWriter = new PdfWriter("../machadostudents-ui/output.pdf");
             PdfDocument pdfDoc = new PdfDocument(pdfWriter);
             Document document = new Document(pdfDoc)) {

            document.setFontSize(FONT_SIZE);

            // Iterate into dates
            outerLoop:
            for (LocalDate date : groupedData.keySet()) {
                Paragraph p=new Paragraph("Fecha: " + date);
                p.setFontSize(12f);
                p.setTextAlignment(TextAlignment.CENTER);
                document.add(p);

                // Iterarate into sections inside each date
                int i=0;
                for (String section : groupedData.get(date).keySet()) {

                    //document.add(pS);

                    // Mapa principal que contendrá los estilos para diferentes valores de i
                    Map<Integer, Map<String, String>> styleMap = new HashMap<>();

                    // Estilos para i=0 y i=4
                    Map<String, String> style0 = new HashMap<>();
                    style0.put("fontColor", "#000000");      // Negro
                    style0.put("bgColor", "#90B1DD");        // Azul claro
                    styleMap.put(0, style0);
                    styleMap.put(4, style0);  // Mismo estilo para i=4

                    // Estilos para i=1
                    Map<String, String> style1 = new HashMap<>();
                    style1.put("fontColor", "#7ABCC6");      // Turquesa más claro
                    style1.put("bgColor", "#2B6C75");        // Turquesa
                    styleMap.put(1, style1);

                    // Estilos para i=2
                    Map<String, String> style2 = new HashMap<>();
                    style2.put("fontColor", "#FFCA64");      // Mostaza más claro
                    style2.put("bgColor", "#936924");        // Mostaza oscuro
                    styleMap.put(2, style2);

                    // Estilos para i=3
                    Map<String, String> style3 = new HashMap<>();
                    style3.put("fontColor", "#CD7473");      // Terracota más claro
                    style3.put("bgColor", "91312D");        // Terracota
                    styleMap.put(3, style3);

                    // Añadir la clave "borderColor" con los valores RGB correspondientes al "bgColor"
                    for (Map.Entry<Integer, Map<String, String>> entry : styleMap.entrySet()) {
                        Map<String, String> styles = entry.getValue();
                        String bgColor = styles.get("bgColor");
                        if (bgColor != null) {
                            int[] rgb = hexToRgb(bgColor);
                            styles.put("borderColor", Arrays.toString(rgb));
                        }
                    }




                    // En caso de que la semana sea sin reunión
                    /*boolean noMeetMark = org.machado.machadostudentsui.views.popups.AssignmentEdit.noMeetMark;
                    if(noMeetMark) continue;*/


                    // Create table
                    Table table = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}));
                    //table.setBorder(Border.NO_BORDER);

                    // Setup columns width
                    table.setWidth(UnitValue.createPercentValue(100));

                    // Section name
                    Paragraph pS = new Paragraph(section);
                    /*switch(i){
                        case 1: //0. Because 0 and four are now PRESIDENCIA Y ORACION FINAL
                            pS.setFontColor(WebColors.getRGBColor("86BFCA"),1); //"86BFCA"
                            break;
                        case 2: //1. Because 0 and four are now PRESIDENCIA Y ORACION FINAL
                            pS.setFontColor(WebColors.getRGBColor("#BA9552"),1); //"#BA9552"
                            break;
                        case 3: //2. Because 0 and four are now PRESIDENCIA Y ORACION FINAL
                            pS.setFontColor(WebColors.getRGBColor("#D27674"),1); //"#D27674"
                            break;
                    }*/
                    String borderColorStr = styleMap.get(i).get("borderColor");
                    int[] borderColorRgb = stringToRgbArray(borderColorStr);
                    DeviceRgb borderColor = new DeviceRgb(borderColorRgb[0], borderColorRgb[1], borderColorRgb[2]);
                    SolidBorder border = new SolidBorder(borderColor, 1);

                    // Cell with section name. Excepts PRESIDENCIA & ORACIÓN FINAL
                    if(i != 0 && i != 4) {
                        Cell cell1 = new Cell(1, 3);
                        //cell1.setWidth(UnitValue.createPointValue(200));
                        cell1.setBackgroundColor(WebColors.getRGBColor(styleMap.get(i).get("bgColor")));
                        //cell1.setBorderLeft(new SolidBorder()); //ColorConstants.WHITE, 1
                        pS.setFontColor(WebColors.getRGBColor(styleMap.get(i).get("fontColor")));




                        cell1.add(pS);
                        table.addCell(cell1);
                        table.setBorder(border);
                    }



                    // Setup headers columns
                    /*String[] encs = {"Intervención", "Encargado", "Ayudante"};
                    for (String e:encs) {
                        Cell cell = new Cell();
                        //cell.setBorder(Border.NO_BORDER);
                        cell.setBackgroundColor(WebColors.getRGBColor("black"));
                        Paragraph pI = new Paragraph(e);
                        pI.setFontColor(WebColors.getRGBColor("white"),0);
                        pI.setBold();
                        cell.add(pI);
                        table.addCell(cell);
                    }*/

                    // Iterate upon records inside each date and section
                    for (Assignment assignment : groupedData.get(date).get(section)) {
                        if(assignment.isWeekWithoutMeet()) {
                            Cell cellWithoutMeet = new Cell(1, 3);
                            cellWithoutMeet.add(new Paragraph("Semana sin reunión"));
                            table.addCell(cellWithoutMeet);
                            //table.addCell("Semana sin reunión");
                            //table.addCell("Semana sin reunión");
                            document.add(table);
                            continue outerLoop;
                        }
                        // Add row to table
                        Cell cellAssignmentName = new Cell();
                        //cell1.setWidth(UnitValue.createPointValue(200));
                        Paragraph pA = new Paragraph(assignment.getName());

                        if(i != 0 && i != 4) {
                            pA.setFontColor(WebColors.getRGBColor(styleMap.get(i).get("bgColor")));

                        } else {
                            cellAssignmentName.setBackgroundColor(WebColors.getRGBColor(styleMap.get(i).get("bgColor")));
                        }
                        cellAssignmentName.add(pA);
                        cellAssignmentName.setBorder(border);

                        table.addCell(cellAssignmentName);
                        //table.addCell(assignment.getName());
                        /*AFTER: String mainName = assignment.getMainStudentName();

                        if (null == assignment.getAssistantStudentName() || assignment.getAssistantStudentName().isEmpty()) {

                            Cell cellMain = new Cell(1, 2);
                            cellMain.add(new Paragraph(mainName));
                            table.addCell(cellMain);

                        }
                        else {

                            String assistantName = assignment.getAssistantStudentName();
                            Cell cellMain = new Cell();
                            cellMain.add(new Paragraph(mainName));
                            table.addCell(cellMain);

                            Cell cellAssistant = new Cell();
                            cellAssistant.add(new Paragraph(assistantName));
                            table.addCell(cellMain);

                        }*/

                        /*BEFORE*/
                        Cell cellMainName = new Cell();
                        Cell cellAssistantName = new Cell();
                        if(i == 0 || i == 4) {
                            cellMainName.setBackgroundColor(WebColors.getRGBColor(styleMap.get(i).get("bgColor")));
                            cellAssistantName.setBackgroundColor(WebColors.getRGBColor(styleMap.get(i).get("bgColor")));
                        }
                        if (null != assignment.getMainStudentName() && !assignment.getMainStudentName().isEmpty()) {
                            Paragraph pN = new Paragraph(assignment.getMainStudentName());
                            cellMainName.add(pN);


                        }
                        else {
                            cellMainName.add(new Paragraph("Pendiente"));
                        }
                        cellMainName.setBorder(border);
                        table.addCell(cellMainName);

                        if (null != assignment.getAssistantStudentName() && !assignment.getMainStudentName().isEmpty()) {
                            Paragraph pH = new Paragraph(assignment.getAssistantStudentName());
                            cellAssistantName.add(pH);

                            cellAssistantName.setBorder(border);
                            table.addCell(pH);


                        } else {
                            cellAssistantName.add(new Paragraph("                    "));

                            cellAssistantName.setBorder(border);
                            table.addCell(cellAssistantName);

                        }

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
        }

    }

    public static void fillForms(List<Assignment> assignments, List<StudentsAssignment> listStudentsAssignment) { //public static void

        try {
            int i = 0;
            for (Assignment assignment : assignments) {
                // Path to template form: Made manually
                String templateForm = "../machadostudents-ui/template/FormatoAsignacionVMC.pdf";

                // Assignments without students will not be generated
                if(!assignment.isWeekWithoutMeet() && assignment.getMainStudentName() != null) {

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
                    //// Fill assistant correctly in form
                    Optional<Student> assistantStudent = filteredListStudentsAssignment.stream()
                            .filter(student -> "assistantStudent".equals(student.getRolStudent()))
                            .map(StudentsAssignment::getStudent)
                            .findFirst();
                    if(assistantStudent.isPresent()) {
                        //Student assistantStudent = getStudentById(assistantStudentId.orElse(0));
                        String nameAssistantStudent = assistantStudent.get().getName() + " " + assistantStudent.get().getLastName() ;
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
