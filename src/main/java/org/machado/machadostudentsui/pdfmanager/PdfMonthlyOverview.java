package org.machado.machadostudentsui.pdfmanager;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.WebColors;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
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
import org.machado.machadostudentsui.utils.SearchUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

import static org.machado.machadostudentsui.utils.FormatUtils.meses;

@Component
public class PdfMonthlyOverview {

    @Value("${outputOverview.path}")
    String outputOverviewPath;

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

    public File getOutputFolder() { //accede al campo scriptPath sin necesidad de pasarla como parámetro, ya que es un campo de instancia de la clase, pues es inyectado por Spring con la anotación @Value
        String basePath = System.getProperty("user.dir"); // Directorio de trabajo actual
        return new File(basePath, outputOverviewPath);  // Combina con la ruta relativa // File asegura el separador correcto
    }



    public String getOutputFileName (String outputOverviewFlexPath, String month, String year) {
        return outputOverviewFlexPath + File.separator + month + year;
    }


    public void generatePDF(Map<LocalDate, Map<String, List<Assignment>>> groupedData, List<StudentsAssignment> listStudentsAssignment) {

        float FONT_SIZE = 8f;
        float INTERLINE = 6f;

        getColorsMap();

        LocalDate firstKey = getFirstKey(groupedData);
        String year = firstKey.getYear()+"";
        String firstMonth = firstKey.getMonth().toString();
        String month = firstMonth.substring(0, 1).toUpperCase() + firstMonth.substring(1).toLowerCase();
        String mes = meses.getOrDefault(month, "MesNoEncontrado");


        File outputFolder = getOutputFolder();
        String outputOverviewFlexPath = outputFolder.getAbsolutePath();
        String outputFileName = getOutputFileName(outputOverviewFlexPath, mes, year);

        try (PdfWriter pdfWriter = new PdfWriter(outputFileName);
             PdfDocument pdfDoc = new PdfDocument(pdfWriter);
             Document document = new Document(pdfDoc)) {;

            document.setCharacterSpacing(0.6f);

            String headerStr = String.format("VIDA & MINISTERIO CRISTIANOS - %s %s", mes.toUpperCase(), year);
            Paragraph pHead = new Paragraph(headerStr); //new Paragraph("VIDA & MINISTERIO CRISTIANOS - " + mes.toUpperCase() + " " + year);
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
                        Map<String, List<StudentsAssignment>> result = SearchUtils.getStudentsAssignmentsByRoom(assignment, listStudentsAssignment);
                        List<StudentsAssignment> listStudentAssignmentPpal = result.get("Ppal");
                        List<StudentsAssignment> listStudentAssignmentAux = result.get("Aux");

                        // Retrieve students in Ppal room
                        Optional<Student> mainStudentPpal = SearchUtils.getStudentInAssignment(listStudentAssignmentPpal, "mainStudent");
                        Optional<Student> assistantStudentPpal = SearchUtils.getStudentInAssignment(listStudentAssignmentPpal, "assistantStudent");

                        // Retrieve students in Aux room
                        Optional<Student> mainStudentAux = SearchUtils.getStudentInAssignment(listStudentAssignmentAux, "mainStudent");
                        Optional<Student> assistantStudentAux = SearchUtils.getStudentInAssignment(listStudentAssignmentAux, "assistantStudent");


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

}