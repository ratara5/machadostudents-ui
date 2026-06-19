# machadostudents-ui

JavaFX desktop frontend for managing congregation meeting assignments. Integrates Spring Boot for dependency injection and service coordination, iText 7 for PDF generation, and external scripts for WhatsApp sharing.

## Dependencies

- Spring Boot (WebFlux, Core, Test)
- JavaFX 13 (graphics, fxml, media)
- iText 7.1.15 (kernel, forms, layout)
- machadostudents-client 1.0
- WireMock 2.27.2 (test)

## Architecture

```
Spring Boot Launcher
  └─ MachadostudentsFxApplication (JavaFX Application)
       └─ MainFrame (sidebar navigation)
            ├─ Home        — Dashboard view
            ├─ Students    — Student CRUD
            ├─ Assignments — Assignment management + PDF/WhatsApp
            └─ Rols        — Role management
```

### Application Flow

1. `MachadostudentsUiApplication.main()` delegates to JavaFX's `Application.launch()`
2. `MachadostudentsFxApplication.init()` bootstraps the Spring context
3. `MachadostudentsFxApplication.start()` sets icons and opens `MainFrame`
4. `MainFrame` loads views into a `StackPane` via sidebar menu clicks
5. Controllers are Spring beans, injected via `FXMLLoader`'s controller factory

## Views

| View        | Controller     | FXML                 | Purpose                                |
|-------------|----------------|----------------------|----------------------------------------|
| Main Frame  | `MainFrame`    | `MainFrame.fxml`     | Sidebar navigation shell               |
| Home        | `Home`         | `Home.fxml`          | Dashboard (placeholder)                |
| Students    | `Students`     | `Student.fxml`       | Student CRUD with search/filter        |
| Assignments | `Assignments`  | `Assignment.fxml`    | Assignment table, PDF gen, WhatsApp    |
| Roles       | `Rols`         | `Rol.fxml`           | Role management                        |
| Dialog      | `Dialog`       | `Dialog.fxml`        | Reusable confirmation dialog           |

### Popups

| Popup                  | Controller            | FXML                      | Purpose                              |
|------------------------|-----------------------|---------------------------|--------------------------------------|
| Student Edit           | `StudentEdit`         | `StudentEdit.fxml`        | Create/edit students                 |
| Role Edit              | `RolEdit`             | `RolEdit.fxml`            | Create/edit roles                    |
| Assignment Edit (Ppal) | `AssignmentEdit`      | `AssignmentEdit.fxml`     | Assign students to main room         |
| Assignment Edit (Aux)  | `AssignmentEditAux`   | `AssignmentEditAux.fxml`  | Assign students to auxiliary room    |

## PDF Generation

Two PDF generators, both managed as Spring `@Component` beans:

### PdfIndividual
- Generates per-assignment PDF cards using an AcroForm template (`FormatoAsignacionVMC.pdf`)
- Fills student names, assignment details, and room checkboxes
- Creates copies for assistant students
- Output: `/output/assignments/`

### PdfMonthlyOverview
- Generates monthly program overviews as structured tables
- Color-coded sections with border styling
- Groups data by date and section
- Marks "Week Without Meeting" entries
- Output: `/output/overview/`

## External Integration

### PDF-to-Image Conversion (Python)
After generating assignment PDFs, the app invokes `output_scripts/convert_pdfs/pdf_to_image.py` to convert each PDF to JPEG for WhatsApp sharing.

### WhatsApp Sender (Node.js)
The `sendAssignments()` method invokes `output_scripts/whatsapp-sender/index.js` which:
1. Authenticates via WhatsApp Web QR code
2. Reads generated JPEG images from `/output/assignments/`
3. Sends each image to the corresponding student's phone number
4. Retries on failure, logs errors, and prevents concurrent execution via file lock

## Navigation

Sidebar menu items are driven by the `Menu` enum (`utils/Menu.java`):

```java
public enum Menu {
    Home("/org/machado/machadostudentsui/views/Home.fxml", "Home"),
    Assignment("/org/machado/machadostudentsui/views/Assignment.fxml", "Assignments"),
    Student("/org/machado/machadostudentsui/views/Student.fxml", "Students"),
    Rol("/org/machado/machadostudentsui/views/Rol.fxml", "Roles"),
    Exit;
}
```

## Configuration

### application.properties

```properties
server.port=8083
outputAssignments.path=/output/assignments
outputOverview.path=/output/overview
scriptsPython.path=/output_scripts/convert_pdfs
scriptsJs.path=/output_scripts/whatsapp-sender
python.path.Linux=/usr/bin/python3.8
python.path.Windows=C:\\Python38\\python.exe
```

## Styling

Custom CSS in `views/style/application.css` with CSS custom properties:

```css
* {
    -primary: #464159;
    -secondary: #6c7b95;
    -primary-light: #8bbabb;
    -secondary-light: #c7f0db;
}
```

## Building & Running

```bash
# From the project root:
mvn -pl machadostudents-ui spring-boot:run

# Or package and run:
mvn -pl machadostudents-ui package -DskipTests
java -jar machadostudents-ui/target/machadostudents-ui-1.0.jar
```

## Backend Requirement

The UI depends on the `machado-ayfm` REST API running on port 8091. Without it, assignment and student data will not load.
