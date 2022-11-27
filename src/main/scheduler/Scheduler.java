package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.*;
import scheduler.util.Util;

import javax.accessibility.AccessibleValue;
import javax.xml.transform.Result;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Arrays;
// David Li
// CSE 414, SP22
public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    // Part 1
    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save the patient information to our database
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    // Part 1
    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    // Part 2
    private static void searchCaregiverSchedule(String[] tokens) {
        // search_caregiver_schedule <date>
        // check 1: make sure a user is logged in
        if(currentCaregiver == null && currentPatient == null){
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens needs to be exactly 2
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAvailable = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
        String getDoses = "SELECT * FROM Vaccines";
        try {
            PreparedStatement statement1 = con.prepareStatement(getAvailable);
            PreparedStatement statement2 = con.prepareStatement(getDoses);
            Date d = Date.valueOf(date);
            statement1.setDate(1, d);
            ResultSet resultSet1 = statement1.executeQuery();
            ResultSet resultSet2 = statement2.executeQuery();

            while(resultSet1.next()) {
                System.out.println(resultSet1.getString("Username") + " ");
            }

            while(resultSet2.next()) {
                System.out.print(resultSet2.getString("Name")+ " ");
                System.out.println(resultSet2.getString("Doses"));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    // Part 2
    private static void reserve(String[] tokens) {
        // reserve <date> <vaccine>
        // check 1: make sure a user is logged in
        if(currentCaregiver == null && currentPatient == null){
            System.out.println("Please login first!");
            return;
        }
        // check 2: make sure user is a patient
        if(currentCaregiver != null && currentPatient == null){
            System.out.println("Please login as a patient!");
            return;
        }
        // check 3: the length for tokens needs to be exactly 3
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        String vaccineName = tokens[2];
        try {
            Date d = Date.valueOf(date);
            Availability availability = new Availability.AvailabilityGetter(d).get();
            // check 4: make sure there is an available caregiver
            if (availability == null) {
                System.out.println("No Caregiver is available!");
                return;
            }
            String availableCaregiver = availability.getCaregiver();

            Vaccine vaccine = new Vaccine.VaccineGetter(vaccineName).get();
            // check 5: make sure vaccine exists
            if (vaccine == null) {
                System.out.println("Invalid Vaccine name! Please try again.");
                return;
            }

            int doses = vaccine.getAvailableDoses();
            // check 6: make sure there are enough doses of the vaccine
            if (doses < 1) {
                System.out.println("Not enough available doses!");
                return;
            }

            // make new entry in Appointments
            Appointment appointment = new Appointment.AppointmentBuilder(d, availableCaregiver,
                                                                    currentPatient.getUsername(), vaccineName).build();
            appointment.makeAppointment();

            // update Availabilities
            availability.deleteAvailability();

            // update Vaccine doses
            vaccine.decreaseAvailableDoses(1);

            System.out.println("Appointment ID: " + appointment.getID() + ", Caregiver username: " + availableCaregiver);

        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    // EC
    private static void cancel(String[] tokens) {
        // cancel <appointment_id>
        // check 1: make sure a user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens needs to be exactly 2
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        try {
            int id = Integer.parseInt(tokens[1]);
            Appointment appointment = new Appointment.AppointmentGetter(id).get();
            // check 3: make sure appointment id exists
            if (appointment == null) {
                System.out.println("Appointment ID does not exist! Please try again.");
                return;
            }

            if (currentCaregiver == null) {
                // check 4: make sure appointment belongs to current patient
                if (!appointment.getPatient().equals(currentPatient.getUsername())) {
                    System.out.println("Please login as a different Patient to cancel this appointment!");
                    return;
                }
            } else {
                // check 5: make sure appointment belongs to current caregiver
                if (!appointment.getCaregiver().equals(currentCaregiver.getUsername())) {
                    System.out.println("Please login as a different Caregiver to cancel this appointment!");
                    return;
                }
            }

            appointment.cancelAppointment(id);
            // update Vaccine doses
            Vaccine vaccine = new Vaccine.VaccineGetter(appointment.getVaccine()).get();
            vaccine.increaseAvailableDoses(1);
            // do not update Availabilities because Caregivers can re-upload if necessary
            System.out.println("Successfully cancelled appointment " + id);

        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid ID!");
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    // Part 2
    private static void showAppointments(String[] tokens) {
        // show_appointments
        // check 1: make sure a user is logged in
        if(currentCaregiver == null && currentPatient == null){
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens needs to be exactly 1
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String patient = "SELECT id, Vaccine, Time, Caregiver FROM Appointments WHERE Patient = ? ORDER BY id ASC";
        String caregiver = "SELECT id, Vaccine, Time, Patient FROM Appointments WHERE Caregiver = ? ORDER BY id ASC";
        try {
            if (currentCaregiver == null) {
                PreparedStatement statement1 = con.prepareStatement(patient);
                statement1.setString(1, currentPatient.getUsername());
                ResultSet resultSet1 = statement1.executeQuery();
                while (resultSet1.next()) {
                    System.out.print(resultSet1.getInt("id") + " ");
                    System.out.print(resultSet1.getString("Vaccine") + " ");
                    System.out.print(resultSet1.getString("Time") + " ");
                    System.out.println(resultSet1.getString("Caregiver"));
                }
            } else {
                PreparedStatement statement2 = con.prepareStatement(caregiver);
                statement2.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet2 = statement2.executeQuery();
                while (resultSet2.next()) {
                    System.out.print(resultSet2.getInt("id") + " ");
                    System.out.print(resultSet2.getString("Vaccine") + " ");
                    System.out.print(resultSet2.getString("Time") + " ");
                    System.out.println(resultSet2.getString("Patient"));
                }
            }
        } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    // Part 2
    private static void logout(String[] tokens) {
        // logout
        // check 1: make sure a user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens needs to be exactly 1
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        if (currentCaregiver == null) {
            currentPatient = null;
            System.out.println("Successfully logged out!");

        } else {
            currentCaregiver = null;
            System.out.println("Successfully logged out!");
        }
    }
}
