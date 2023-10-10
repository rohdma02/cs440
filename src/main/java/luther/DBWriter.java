package luther;

import java.util.Random;
import com.github.javafaker.Faker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import mlb.DatabaseWriter;

import java.io.File;

import java.io.FileNotFoundException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBWriter {
    private static final String DATABASE_URL = "jdbc:sqlite:data/luther/luther.sqlite";

    public static void main(String[] args) throws IOException {
        try {

            Connection conn = DriverManager.getConnection(DATABASE_URL);

            createTables(conn);
            populateDepartment(conn, "data/luther/departments.txt");
            populateLocation(conn, "data/luther/buildings.txt");
            populateMajor(conn, "data/luther/programs-majors.txt");
            populateFaculty(conn, "data/luther/faculty.txt");
            populateStudent(conn);
            populateEnrollment(conn);
            populateSemester(conn);

            String courseHtmlUrl = "http://www.faculty.luther.edu/~bernatzr/Registrar-Public/Course%20Enrollments/enrollments_FA2023.htm";
            populateSection(conn, courseHtmlUrl);
            populateCourse(conn, courseHtmlUrl);

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        conn.createStatement().execute("DROP TABLE IF EXISTS Department;");
        conn.createStatement().execute("DROP TABLE IF EXISTS Location;");
        conn.createStatement().execute("DROP TABLE IF EXISTS Major;");
        conn.createStatement().execute("DROP TABLE IF EXISTS Student;");
        conn.createStatement().execute("DROP TABLE IF EXISTS Enrollment;");
        conn.createStatement().execute("DROP TABLE IF EXISTS Semester;");
        conn.createStatement().execute("DROP TABLE IF EXISTS Section;");
        conn.createStatement().execute("DROP TABLE IF EXISTS Faculty;");
        conn.createStatement().execute("DROP TABLE IF EXISTS Course;");

        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Department (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "building TEXT, " +
                "head);");

        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Location (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "building TEXT," +
                "room INT," +
                "purpose TEXT);");

        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Major (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "department INT," +
                "name TEXT);");

        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Student (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "graduationDate INT," +
                "major INT," +
                "adviser INT);");

        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Enrollment (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "student INT," +
                "section INT," +
                "grade TEXT);");

        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Semester (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "year INT," +
                "season TEXT);");

        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Section (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "course INT," +
                "instructor INT," +
                "offered INT," +
                "location INT," +
                "startHour TIME);");

        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Faculty (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "department INT," +
                "startDate INT," +
                "endDate INT," +
                "office INT);");

        conn.createStatement().execute("CREATE TABLE IF NOT EXISTS Course (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "department INT," +
                "abbreviation TEXT," +
                "number INT," +
                "title TEXT," +
                "credits INT);");

        conn.createStatement().execute("PRAGMA foreign_keys=ON;");
    }

    private static void populateDepartment(Connection conn, String filename) throws SQLException {
        try {
            Scanner fs = new Scanner(new File(filename));
            String departmentName = null;
            String departmentBuilding = null;
            String departmentHead = null;
            String line;

            while (fs.hasNextLine()) {
                line = fs.nextLine();
                if (line.startsWith("Departments")) {
                    continue;
                } else if (line.isEmpty()) {
                    if (fs.hasNextLine()) {
                        departmentName = fs.nextLine();
                        if (fs.hasNextLine()) {
                            departmentHead = fs.nextLine();

                            if (fs.hasNextLine()) {
                                fs.nextLine();
                                if (fs.hasNextLine()) {
                                    departmentBuilding = fs.nextLine();
                                    insertDepartment(conn, departmentName, departmentBuilding, departmentHead);
                                }
                            }
                        }
                    }
                }
            }
            fs.close();
        } catch (IOException ex) {
            Logger.getLogger(DatabaseWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void insertDepartment(Connection conn, String name, String building, String head)
            throws SQLException {
        String sql = "INSERT INTO Department (name, building, head) VALUES (?, ?, ?);";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        pstmt.setString(2, building);
        pstmt.setString(3, head);

        pstmt.executeUpdate();
    }

    private static void populateLocation(Connection conn, String filename) throws SQLException {
        try {
            Scanner fs = new Scanner(new File(filename));
            String[] purposes = {
                    "Office",
                    "Classroom",
                    "Meeting Room",
                    "Lab" };
            String building = null;
            int room = 0;
            String purpose = null;
            Random random = new Random();

            while (fs.hasNextLine()) {
                building = fs.nextLine();
                room = random.nextInt(100) + 1;
                purpose = purposes[random.nextInt(purposes.length)];
                insertLocation(conn, building, room, purpose);
            }

            fs.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void insertLocation(Connection conn, String building, int room, String purpose) throws SQLException {
        String sql = "INSERT INTO Location (building, room, purpose) VALUES (?, ?, ?);";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, building);
        pstmt.setInt(2, room);
        pstmt.setString(3, purpose);
        pstmt.executeUpdate();
    }

    private static void populateMajor(Connection conn, String filename) throws SQLException {
        try {
            Scanner fs = new Scanner(new File(filename));
            int department = 0;
            String name = null;

            while (fs.hasNextLine()) {
                String line = fs.nextLine();

                while (line.startsWith("BIOLOGY")) {
                    department = 1;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }

                while (line.startsWith("CHEMISTRY")) {
                    department = 2;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                }

                while (line.startsWith("COMMUNICATION STUDIES")) {
                    department = 3;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);

                }
                while (line.startsWith("COMPUTER SCIENCE")) {
                    department = 4;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);

                }
                while (line.startsWith("ECONOMICS, ACCOUNTING, AND MANAGEMENT")) {
                    department = 5;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("EDUCATION")) {
                    department = 6;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("ENGLISH")) {
                    department = 7;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("ENVIRONMENTAL STUDIES")) {
                    department = 8;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                }
                while (line.startsWith("HEALTH AND EXERCISE SCIENCE")) {
                    department = 9;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("HISTORY")) {
                    department = 10;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);

                    line = fs.nextLine();

                }
                while (line.startsWith("IDENTITY STUDIES")) {
                    department = 11;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);

                }
                while (line.startsWith("INTERNATIONALSTUDIES")) {
                    department = 12;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("MATHS")) {
                    department = 13;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                }
                while (line.startsWith("MODERN LANGUAGES AND CULTURES")) {
                    department = 14;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("MUSICD")) {
                    department = 15;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("NURSINGD")) {
                    department = 16;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("PAIDEIAD")) {
                    department = 17;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("PHILOSOPHYD")) {
                    department = 18;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("PHYSICSD")) {
                    department = 19;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("POLITICALSCIENCE")) {
                    department = 20;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("PSYCHOLOGYD")) {
                    department = 21;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);

                }
                while (line.startsWith("RELIGIOND")) {
                    department = 22;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                }
                while (line.startsWith("SOCIOLOGY, ANTHROPOLOGY, AND SOCIAL WORK")) {
                    department = 23;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);

                }
                while (line.startsWith("VISUAL AND PERFMORMING ARTS")) {
                    department = 24;
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);
                    line = fs.nextLine();
                    name = line;
                    insertMajor(conn, department, name);

                }

            }

            fs.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void insertMajor(Connection conn, int department, String name) throws SQLException {
        String sql = "INSERT INTO Major (department, name) VALUES (?,?);";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, department);
        pstmt.setString(2, name);
        pstmt.executeUpdate();
    }

    private static void populateStudent(Connection conn) {
        Faker faker = new Faker();
        Random random = new Random();

        try {
            String sql = "INSERT INTO Student (name, graduationDate, major, adviser) VALUES (?, ?, ?, ?);";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            int numStudents = 250;

            for (int i = 0; i < numStudents; i++) {
                String name = faker.name().firstName() + " " + faker.name().lastName();
                int graduationDate = random.nextInt(8) + 2020;
                int majorId = random.nextInt(50) + 1;
                int adviserId = random.nextInt(50) + 1;

                pstmt.setString(1, name);
                pstmt.setInt(2, graduationDate);
                pstmt.setInt(3, majorId);
                pstmt.setInt(4, adviserId);

                pstmt.executeUpdate();
            }

            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void populateFaculty(Connection conn, String filename) throws SQLException {
        try {
            Scanner fs = new Scanner(new File(filename));
            Random random = new Random();

            int department = 0;
            String line;

            while (fs.hasNextLine()) {
                line = fs.nextLine().trim();

                if (line.startsWith("Biology")) {
                    department = 1;
                } else if (line.startsWith("Chemistry")) {
                    department += 1;
                } else if (line.startsWith("Communication Studies")) {
                    department += 1;
                } else if (line.startsWith("Computer Science")) {
                    department = 4;
                } else if (line.startsWith("Economics Accounting & Management")) {
                    department += 1;
                } else if (line.startsWith("Education")) {
                    department += 1;
                } else if (line.startsWith("English")) {
                    department += 1;
                } else if (line.startsWith("Environmental Studies")) {
                    department += 1;
                } else if (line.startsWith("History")) {
                    department += 1;
                } else if (line.startsWith("Health & Exercise Science")) {
                    department += 1;
                } else if (line.startsWith("Identity Studies")) {
                    department += 1;
                } else if (line.startsWith("Library")) {
                    department += 1;
                } else if (line.startsWith("Mathematics")) {
                    department += 1;
                } else if (line.startsWith("Modern Languages and Cultures")) {
                    department += 1;
                } else if (line.startsWith("Music")) {
                    department += 1;
                } else if (line.startsWith("Nursing")) {
                    department += 1;
                } else if (line.startsWith("Paideia")) {
                    department += 1;
                } else if (line.startsWith("Philosophy")) {
                    department += 1;
                } else if (line.startsWith("Physics")) {
                    department += 1;
                } else if (line.startsWith("Political Science")) {
                    department += 1;
                } else if (line.startsWith("Psychology")) {
                    department += 1;
                } else if (line.startsWith("Religion")) {
                    department += 1;
                } else if (line.startsWith("Social Work Anthropology & Sociology")) {
                    department += 1;
                } else if (line.startsWith("Visual & Performing Arts")) {
                    department += 1;
                } else if (!line.isEmpty()) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 5) {
                        String name = parts[0] + " " + parts[1];
                        String office = parts[2] + " " + parts[3];
                        int startDate = random.nextInt(33) + 1990;
                        int endtDate = 2030;

                        insertFaculty(conn, name, department, office, startDate, endtDate);
                    }
                }
            }

            fs.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void insertFaculty(Connection conn, String name, int department, String office, int startDate,
            Integer endDate) throws SQLException {
        String sql = "INSERT INTO Faculty (name, department, office, startDate, endDate) VALUES (?, ?, ?, ?, ?);";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        pstmt.setInt(2, department);
        pstmt.setString(3, office);
        pstmt.setInt(4, startDate);
        pstmt.setString(5, null);
        pstmt.executeUpdate();
    }

    private static void populateEnrollment(Connection conn) {
        try {
            String sql = "INSERT INTO Enrollment (student, section, grade) VALUES (?, ?,?);";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            Random random = new Random();

            int numEnrollments = 250;

            for (int i = 0; i < numEnrollments; i++) {
                int studentId = getRandomStudentId(conn);
                int sectionId = random.nextInt(943);
                String grade = getRandomGrade();

                pstmt.setInt(1, studentId);
                pstmt.setInt(2, sectionId);
                pstmt.setString(3, grade);

                pstmt.executeUpdate();
            }

            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int getRandomStudentId(Connection conn) throws SQLException {
        String sql = "SELECT id FROM Student ORDER BY RANDOM() LIMIT 1;";
        Statement stmt = conn.createStatement();
        ResultSet resultSet = stmt.executeQuery(sql);

        if (resultSet.next()) {
            return resultSet.getInt("id");
        }
        return 0;

    }

    private static String getRandomGrade() {
        Random random = new Random();
        int probability = random.nextInt(100);

        if (probability < 80) {
            return getRandomLetterGrade();
        } else {
            return null;
        }
    }

    private static String getRandomLetterGrade() {
        String[] letterGrades = { "A", "B", "C", "D", "F" };
        String[] modifiers = { "", "+", "-" };

        Random random = new Random();
        int gradeIndex = random.nextInt(letterGrades.length);

        int modifierIndex = random.nextInt(modifiers.length);

        String grade = letterGrades[gradeIndex];
        String modifier = modifiers[modifierIndex];

        return grade + modifier;
    }

    private static void populateSemester(Connection conn) {
        try {

            String sql = "INSERT INTO Semester (year, season) VALUES (?, ?);";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            int academicYear = 2023;
            String[] seasons = { "Fall", "J-Term", "Spring" };

            for (String season : seasons) {
                pstmt.setInt(1, academicYear);
                academicYear = 2024;
                pstmt.setString(2, season);
                pstmt.setString(2, season);

                pstmt.executeUpdate();
            }

            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void populateCourse(Connection conn, String url) throws SQLException, IOException {
        Document doc = Jsoup.connect(url).get();
        String[] courses = { "BIO", "CHEM", "COMS", "CS", "EAM", "ENG", "EDUC",
                "ENVS", "GH", "GS", "HIST", "HPE", "IDS",
                "IEDU", "LV", "MATH", "MLLL", "MUS", "MUST",
                "NEUR", "NORST", "NURS", "PAID", "PHIL",
                "PHYS", "POLS", "PSYC", "REL", "SCI", "SASW", "VC", "VPA" };

        Set<String> insertedCourses = new HashSet<>();
        Elements tables = doc.select("table");

        for (String course : courses) {
            int departmentC = 0;
            if (course == "BIO") {

                for (Element table : tables) {
                    String tableText = table.text();
                    int department = 1;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "CHEM") {

                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "COMS") {

                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "CS") {

                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "EAM") {

                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "ENG") {
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "EDUC") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "ENVS") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "GH") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "GS") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "HIST") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "HPE") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "IDS") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "IEDU") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "LV") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "MATH") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "MLLL") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "MUS") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "MUST") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "NEUR") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "NORST") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "NURS") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "PAID") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "PHIL") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "PHYS") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "POLS") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "PSYC") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();
                    int department = departmentC++;
                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {

                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                String courseKey = abbreviation + number;
                                if (!insertedCourses.contains(courseKey)) {
                                    int credits = 4;
                                    insertCourse(conn, department, abbreviation, number, title, credits);
                                    insertedCourses.add(courseKey);
                                }

                            }
                        }
                    }
                }
            } else if (course == "REL") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();

                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {
                                int department = departmentC;
                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                int credits = 4;
                                insertCourse(conn, department, abbreviation, number, title, credits);
                            }
                        }
                    }
                }
            } else if (course == "SCI") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();

                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {
                                int department = departmentC;
                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                int credits = 4;
                                insertCourse(conn, department, abbreviation, number, title, credits);
                            }
                        }
                    }
                }
            } else if (course == "SASW") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();

                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {
                                int department = departmentC;
                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                int credits = 4;
                                insertCourse(conn, department, abbreviation, number, title, credits);
                            }
                        }
                    }
                }
            } else if (course == "VC") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();

                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {
                                int department = departmentC;
                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                int credits = 4;
                                insertCourse(conn, department, abbreviation, number, title, credits);
                            }
                        }
                    }
                }
            } else if (course == "VPA") {
                departmentC++;
                for (Element table : tables) {
                    String tableText = table.text();

                    if (tableText.contains(course)) {
                        Elements rows = table.select("tr");

                        for (Element row : rows) {
                            Elements columns = row.select("td");

                            if (columns.size() >= 4) {
                                int department = departmentC;
                                String abbr = columns.get(0).text();
                                String[] parts = abbr.split("-");
                                String abbreviation = parts[0];
                                String numberString = parts[1];
                                if (numberString.length() > 3) {
                                    numberString = parts[1].substring(0, parts[1].length() - 1);
                                } else {
                                    numberString = parts[1];
                                }
                                Integer number = Integer.valueOf(numberString);
                                String title = columns.get(1).text();
                                int credits = 4;
                                insertCourse(conn, department, abbreviation, number, title, credits);
                            }
                        }
                    }
                }
            }

        }
    }

    private static void insertCourse(Connection conn, int department, String abbreviation,
            int number, String title, int credits) throws SQLException {
        String sql = "INSERT INTO Course (department, abbreviation, number, title, credits) VALUES (?, ?, ?, ?, ?);";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, department);
        pstmt.setString(2, abbreviation);
        pstmt.setInt(3, number);
        pstmt.setString(4, title);
        pstmt.setInt(5, credits);
        pstmt.executeUpdate();
    }

    private static void populateSection(Connection conn, String url) throws SQLException, IOException {
        Document doc = Jsoup.connect(url).get();
        String[] courses = { "BIO", "CHEM", "COMS", "CS", "EAM", "ENG", "EDUC",
                "ENVS", "GH", "GS", "HIST", "HPE", "IDS",
                "IEDU", "LV", "MATH", "MLLL", "MUS", "MUST",
                "NEUR", "NORST", "NURS", "PAID", "PHIL",
                "PHYS", "POLS", "PSYC", "REL", "SCI", "SASW", "VC", "VPA" };

        Random random = new Random();

        Elements tables = doc.select("table");

        for (String course : courses) {
            for (Element table : tables) {
                String tableText = table.text();

                if (tableText.contains(course)) {
                    Elements rows = table.select("tr");

                    for (Element row : rows) {
                        Elements columns = row.select("td");

                        if (columns.size() >= 4) {
                            String courseName = columns.get(0).text();
                            String courseInst = columns.get(2).text();

                            int courseOffered = random.nextInt(3) + 1;

                            String courseLocation = columns.get(14).text();
                            String courseStartHour = columns.get(6).text() + columns.get(7).text();

                            insertSection(conn, courseName, courseInst, courseOffered, courseLocation,
                                    courseStartHour);
                        }
                    }
                }
            }
        }
    }

    private static void insertSection(Connection conn, String courseName, String courseInst, int courseOffered,
            String courseLocation, String courseStartHour)
            throws SQLException {
        String sql = "INSERT INTO Section (course, instructor, offered, location, startHour) VALUES (?, ?, ?, ?, ?);";
        PreparedStatement pstmt = conn.prepareStatement(sql);

        pstmt.setString(1, courseName);
        pstmt.setString(2, courseInst);
        pstmt.setInt(3, courseOffered);
        pstmt.setString(4, courseLocation);
        pstmt.setString(5, courseStartHour);

        pstmt.executeUpdate();
    }

}
