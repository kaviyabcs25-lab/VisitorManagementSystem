import java.awt.*;
import java.awt.event.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

/*
 * Visitor Management System with Family and Overstay Monitoring
 * Save as: VisitorManagementSystemAWT.java
 *
 * Compile: javac VisitorManagementSystemAWT.java
 * Run:     java VisitorManagementSystemAWT
 *
 * Note: This version stores records in memory. Records are cleared when
 * the application closes. Database/file storage can be added separately.
 */

public class VisitorManagementSystemAWT extends Frame implements ActionListener {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a", Locale.ENGLISH);

    private static class Visit {
        String visitorId;
        String name;
        String phone;
        String alternateId;
        int familyMembers;
        int allowedMinutes;
        LocalDateTime checkIn;
        LocalDateTime expectedExit;
        LocalDateTime checkOut;

        Visit(String visitorId, String name, String phone, String alternateId,
              int familyMembers, int allowedMinutes, LocalDateTime checkIn) {
            this.visitorId = visitorId;
            this.name = name;
            this.phone = phone;
            this.alternateId = alternateId;
            this.familyMembers = familyMembers;
            this.allowedMinutes = allowedMinutes;
            this.checkIn = checkIn;
            this.expectedExit = checkIn.plusMinutes(allowedMinutes);
            this.checkOut = null;
        }

        boolean isCheckedOut() {
            return checkOut != null;
        }

        boolean isOverdue(LocalDateTime now) {
            return !isCheckedOut() && now.isAfter(expectedExit);
        }
    }

    private final ArrayList<Visit> visits = new ArrayList<Visit>();
    private int nextId = 1;

    // Registration fields
    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField alternateIdField = new TextField();
    private final TextField familyCountField = new TextField("1");
    private final TextField durationField = new TextField("60");

    // Search / checkout field
    private final TextField visitorIdField = new TextField();
    private final TextField searchField = new TextField();

    private final Choice searchByChoice = new Choice();
    private final TextArea output = new TextArea("", 18, 100,
            TextArea.SCROLLBARS_BOTH);
    private final Label status = new Label("Ready");

    private final Button registerButton = new Button("Register Visit");
    private final Button checkoutButton = new Button("Check Out");
    private final Button viewButton = new Button("View All Visits");
    private final Button searchButton = new Button("Search Visitor");
    private final Button overdueButton = new Button("Check Overdue");
    private final Button reportButton = new Button("Generate Report");
    private final Button clearButton = new Button("Clear Fields");
    private final Button activeButton = new Button("View Active Visits");

    public VisitorManagementSystemAWT() {
        setTitle("Visitor Management System");
        setSize(1100, 760);
        setLayout(new BorderLayout(8, 8));

        Label heading = new Label(
                "VISITOR MANAGEMENT SYSTEM", Label.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 21));

        Panel form = new Panel(new GridLayout(0, 4, 7, 7));
        form.add(new Label("Primary Visitor Name:"));
        form.add(nameField);
        form.add(new Label("Phone Number (optional):"));
        form.add(phoneField);

        form.add(new Label("Alternate ID / Pass Reference:"));
        form.add(alternateIdField);
        form.add(new Label("Family Members (including visitor):"));
        form.add(familyCountField);

        form.add(new Label("Allowed Stay (minutes):"));
        form.add(durationField);
        form.add(new Label("Visitor ID (for checkout):"));
        form.add(visitorIdField);

        form.add(new Label("Search Value:"));
        form.add(searchField);
        form.add(new Label("Search By:"));
        searchByChoice.add("Visitor ID");
        searchByChoice.add("Name");
        searchByChoice.add("Phone");
        searchByChoice.add("Alternate ID");
        form.add(searchByChoice);

        Panel buttons = new Panel(new GridLayout(2, 4, 6, 6));
        Button[] buttonList = {
                registerButton, checkoutButton, viewButton, searchButton,
                overdueButton, reportButton, activeButton, clearButton
        };
        for (Button button : buttonList) {
            buttons.add(button);
            button.addActionListener(this);
        }

        Panel top = new Panel(new BorderLayout(6, 6));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        Panel northPanel = new Panel(new BorderLayout(6, 6));
        northPanel.add(heading, BorderLayout.NORTH);
        northPanel.add(top, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);

        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(output, BorderLayout.CENTER);

        status.setFont(new Font("Arial", Font.BOLD, 13));
        add(status, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
        showDashboard();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == registerButton) {
            registerVisit();
        } else if (source == checkoutButton) {
            checkOutVisitor();
        } else if (source == viewButton) {
            viewAllVisits();
        } else if (source == searchButton) {
            searchVisitor();
        } else if (source == overdueButton) {
            showOverdueVisits();
        } else if (source == reportButton) {
            generateReport();
        } else if (source == activeButton) {
            viewActiveVisits();
        } else if (source == clearButton) {
            clearFields();
        }
    }

    // MODULE 1: Register an individual or family visit
    private void registerVisit() {
        try {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String alternateId = alternateIdField.getText().trim();

            if (name.isEmpty()) {
                throw new IllegalArgumentException("Visitor name is required.");
            }
            if (!phone.isEmpty() && !phone.matches("\\d{10}")) {
                throw new IllegalArgumentException(
                        "Phone must contain exactly 10 digits, or leave it blank.");
            }
            if (phone.isEmpty() && alternateId.isEmpty()) {
                throw new IllegalArgumentException(
                        "Enter a phone number or an alternate ID/pass reference.");
            }

            int familyMembers = Integer.parseInt(familyCountField.getText().trim());
            int allowedMinutes = Integer.parseInt(durationField.getText().trim());

            if (familyMembers < 1 || familyMembers > 100) {
                throw new IllegalArgumentException(
                        "Family member count must be between 1 and 100.");
            }
            if (allowedMinutes < 1 || allowedMinutes > 1440) {
                throw new IllegalArgumentException(
                        "Allowed stay must be between 1 and 1440 minutes.");
            }

            String id = String.format("VMS%04d", nextId++);
            LocalDateTime now = LocalDateTime.now();
            Visit visit = new Visit(id, name, phone, alternateId,
                    familyMembers, allowedMinutes, now);
            visits.add(visit);

            output.setText("VISIT REGISTERED SUCCESSFULLY\n\n"
                    + formatVisit(visit, LocalDateTime.now())
                    + "\nVisitor ID: " + id
                    + "\nPlease keep this Visitor ID for checkout and future search.");
            status.setText("Registered successfully. Visitor ID: " + id);

        } catch (NumberFormatException ex) {
            status.setText("Enter valid whole numbers for family count and duration.");
        } catch (IllegalArgumentException ex) {
            status.setText(ex.getMessage());
        }
    }

    // MODULE 2: Record exit time using the generated Visitor ID
    private void checkOutVisitor() {
        String id = visitorIdField.getText().trim();
        if (id.isEmpty()) {
            status.setText("Enter the Visitor ID to check out.");
            return;
        }

        Visit visit = findById(id);
        if (visit == null) {
            status.setText("Visitor ID not found.");
            return;
        }
        if (visit.isCheckedOut()) {
            status.setText("This visitor/group has already checked out.");
            output.setText(formatVisit(visit, LocalDateTime.now()));
            return;
        }

        visit.checkOut = LocalDateTime.now();
        long actualMinutes = Duration.between(visit.checkIn, visit.checkOut).toMinutes();

        output.setText("CHECKOUT RECORDED SUCCESSFULLY\n\n"
                + formatVisit(visit, LocalDateTime.now())
                + "\nActual stay: " + actualMinutes + " minutes"
                + (visit.checkOut.isAfter(visit.expectedExit)
                    ? "\nOVERSTAY: " + (actualMinutes - visit.allowedMinutes)
                        + " minute(s) beyond the allowed duration."
                    : "\nStatus: Checked out within the allowed duration."));
        status.setText("Checkout recorded for " + visit.visitorId);
    }

    // MODULE 3: Display every registered visit
    private void viewAllVisits() {
        if (visits.isEmpty()) {
            output.setText("No visit records available.");
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("ALL VISIT RECORDS\n");
        result.append("============================================================\n");
        for (Visit visit : visits) {
            result.append(formatVisit(visit, LocalDateTime.now()));
            result.append("\n------------------------------------------------------------\n");
        }
        output.setText(result.toString());
        status.setText("Displayed " + visits.size() + " visit record(s).");
    }

    // MODULE 4: Search using Visitor ID, name, phone or alternate reference
    private void searchVisitor() {
        String key = searchField.getText().trim();
        if (key.isEmpty()) {
            status.setText("Enter a value to search.");
            return;
        }

        String mode = searchByChoice.getSelectedItem();
        StringBuilder result = new StringBuilder();
        int matches = 0;

        for (Visit visit : visits) {
            boolean found = false;
            if ("Visitor ID".equals(mode)) {
                found = visit.visitorId.equalsIgnoreCase(key);
            } else if ("Name".equals(mode)) {
                found = visit.name.toLowerCase(Locale.ENGLISH)
                        .contains(key.toLowerCase(Locale.ENGLISH));
            } else if ("Phone".equals(mode)) {
                found = !visit.phone.isEmpty() && visit.phone.equals(key);
            } else if ("Alternate ID".equals(mode)) {
                found = !visit.alternateId.isEmpty()
                        && visit.alternateId.equalsIgnoreCase(key);
            }

            if (found) {
                result.append(formatVisit(visit, LocalDateTime.now()));
                result.append("\n------------------------------------------------------------\n");
                matches++;
            }
        }

        if (matches == 0) {
            output.setText("No matching visitor record found.");
            status.setText("No matching record.");
        } else {
            output.setText("SEARCH RESULTS: " + matches + "\n\n" + result);
            status.setText("Found " + matches + " matching record(s).");
        }
    }

    // MODULE 5: List visits that have exceeded their allowed duration
    private void showOverdueVisits() {
        LocalDateTime now = LocalDateTime.now();
        StringBuilder result = new StringBuilder();
        int overdueGroups = 0;
        int overduePeople = 0;

        for (Visit visit : visits) {
            if (visit.isOverdue(now)) {
                overdueGroups++;
                overduePeople += visit.familyMembers;
                result.append(formatVisit(visit, now));
                result.append("\nOVERDUE BY: ")
                        .append(Duration.between(visit.expectedExit, now).toMinutes())
                        .append(" minute(s)\n");
                result.append("------------------------------------------------------------\n");
            }
        }

        if (overdueGroups == 0) {
            output.setText("NO OVERDUE VISITS\nAll active visitors are within their allowed duration.");
            status.setText("No overdue visits found.");
        } else {
            output.setText("OVERDUE ALERT\n"
                    + "Overdue groups: " + overdueGroups
                    + "\nPeople in overdue groups: " + overduePeople
                    + "\n\n" + result);
            status.setText("ALERT: " + overdueGroups + " group(s) have exceeded their time.");
        }
    }

    // MODULE 6: Show visitors/groups that have not checked out
    private void viewActiveVisits() {
        LocalDateTime now = LocalDateTime.now();
        StringBuilder result = new StringBuilder();
        int groups = 0;
        int people = 0;

        for (Visit visit : visits) {
            if (!visit.isCheckedOut()) {
                groups++;
                people += visit.familyMembers;
                result.append(formatVisit(visit, now));
                result.append("\n------------------------------------------------------------\n");
            }
        }

        output.setText("ACTIVE VISITS\nActive groups: " + groups
                + "\nPeople currently registered as inside: " + people
                + "\n\n" + (groups == 0 ? "No active visits." : result.toString()));
        status.setText("Active visit list displayed.");
    }

    // MODULE 7: Generate visitor and family report
    private void generateReport() {
        LocalDateTime now = LocalDateTime.now();
        String today = now.format(DATE_FORMAT);

        int totalGroups = 0;
        int totalPeople = 0;
        int todaysGroups = 0;
        int todaysPeople = 0;
        int activeGroups = 0;
        int activePeople = 0;
        int overdueGroups = 0;
        int overduePeople = 0;
        int checkedOutGroups = 0;

        for (Visit visit : visits) {
            totalGroups++;
            totalPeople += visit.familyMembers;

            if (visit.checkIn.format(DATE_FORMAT).equals(today)) {
                todaysGroups++;
                todaysPeople += visit.familyMembers;
            }
            if (visit.isCheckedOut()) {
                checkedOutGroups++;
            } else {
                activeGroups++;
                activePeople += visit.familyMembers;
                if (visit.isOverdue(now)) {
                    overdueGroups++;
                    overduePeople += visit.familyMembers;
                }
            }
        }

        output.setText("========== VISITOR MANAGEMENT REPORT ==========\n\n"
                + "Report Date: " + today
                + "\nTotal Visit / Family Groups: " + totalGroups
                + "\nTotal People Registered: " + totalPeople
                + "\n\nTODAY'S VISITS"
                + "\nGroups visited today: " + todaysGroups
                + "\nPeople visited today: " + todaysPeople
                + "\n\nCURRENT STATUS"
                + "\nActive groups: " + activeGroups
                + "\nPeople currently inside (based on records): " + activePeople
                + "\nChecked-out groups: " + checkedOutGroups
                + "\nOverdue groups: " + overdueGroups
                + "\nPeople in overdue groups: " + overduePeople
                + "\n\nNote: People count is the family-member count entered during registration.");
        status.setText("Report generated successfully.");
    }

    private Visit findById(String id) {
        for (Visit visit : visits) {
            if (visit.visitorId.equalsIgnoreCase(id)) {
                return visit;
            }
        }
        return null;
    }

    private String formatVisit(Visit visit, LocalDateTime now) {
        String exitText = visit.isCheckedOut()
                ? visit.checkOut.format(DATE_TIME_FORMAT)
                : "Not checked out";

        String state;
        if (visit.isCheckedOut()) {
            state = "CHECKED OUT";
        } else if (visit.isOverdue(now)) {
            state = "OVERDUE";
        } else {
            state = "ACTIVE";
        }

        return "Visitor ID: " + visit.visitorId
                + "\nPrimary Visitor: " + visit.name
                + "\nPhone: " + (visit.phone.isEmpty() ? "Not provided" : visit.phone)
                + "\nAlternate ID / Pass: "
                + (visit.alternateId.isEmpty() ? "Not provided" : visit.alternateId)
                + "\nFamily Members: " + visit.familyMembers
                + "\nVisit Date: " + visit.checkIn.format(DATE_FORMAT)
                + "\nEntry Date & Time: " + visit.checkIn.format(DATE_TIME_FORMAT)
                + "\nExpected Exit: " + visit.expectedExit.format(DATE_TIME_FORMAT)
                + "\nActual Exit: " + exitText
                + "\nAllowed Stay: " + visit.allowedMinutes + " minutes"
                + "\nCurrent Status: " + state;
    }

    private void showDashboard() {
        output.setText("WELCOME!\n\n"
                + "1. Enter the primary visitor's name.\n"
                + "2. Enter a phone number OR an alternate ID/pass reference.\n"
                + "3. Enter the total family-member count (include the primary visitor).\n"
                + "4. Enter the allowed stay in minutes.\n"
                + "5. Click Register Visit. The system generates a unique Visitor ID.\n"
                + "6. Use that Visitor ID to record checkout.\n\n"
                + "Use 'Check Overdue' to identify active visits beyond their allowed time.");
    }

    private void clearFields() {
        nameField.setText("");
        phoneField.setText("");
        alternateIdField.setText("");
        familyCountField.setText("1");
        durationField.setText("60");
        visitorIdField.setText("");
        searchField.setText("");
        output.setText("");
        status.setText("Fields cleared.");
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new VisitorManagementSystemAWT();
            }
        });
    }
}
