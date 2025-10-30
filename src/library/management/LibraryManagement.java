package library.management;

import config.config;
import java.util.*;

public class LibraryManagement {

    // View all books
    public static void viewbook(config db) {
        String query = "SELECT * FROM tbl_Librarian";
        String[] headers = {"bid", "title", "names", "date", "genre"};
        String[] columns = {"bid", "title", "names", "date", "genre"};
        db.viewRecords(query, headers, columns);
    }

    // View users waiting approval
    public static void viewUsers(config db) {
        String query = "SELECT * FROM tbl_log WHERE u_status != 'Approved'";
        String[] headers = {"u_id", "u_email", "u_type", "u_status"};
        String[] columns = {"u_id", "u_email", "u_type", "u_status"};
        db.viewRecords(query, headers, columns);
    }

    // View penalties
    public static void viewPenalties(config db) {
        String query = "SELECT * FROM tbl_penalty";
        String[] headers = {"penalty_id", "b_name", "b_email", "b_id", "b_days", "b_penal", "b_dpenal"};
        String[] columns = {"penalty_id", "b_name", "b_email", "b_id", "b_days", "b_penal", "b_dpenal"};
        db.viewRecords(query, headers, columns);
    }

    // Show all borrowed books with borrower and book details
    public static void viewAllBorrowedBooksWithDetails(config db) {
        String query = "SELECT DISTINCT tl.b_name, tb.borrower_email, tb.book_id, tl2.title, tb.borrow_date " +
                "FROM tbl_borrowed tb " +
                "JOIN tbl_log tl ON tb.borrower_email = tl.u_email " +
                "JOIN tbl_Librarian tl2 ON tb.book_id = tl2.bid " +
                "WHERE tb.status = 'Borrowed'";
        String[] headers = {"borrower_name", "email", "book_id", "book_title", "borrow_date"};
        String[] columns = {"b_name", "borrower_email", "book_id", "title", "borrow_date"};
        db.viewRecords(query, headers, columns);
    }

    // View borrowed books for a specific user
    public static void viewBorrowedBooks(config db, String email) {
        String query = "SELECT tb.book_id, tl.title, tb.borrow_date " +
                "FROM tbl_borrowed tb " +
                "JOIN tbl_Librarian tl ON tb.book_id = tl.bid " +
                "WHERE tb.borrower_email = '" + email + "' AND tb.status = 'Borrowed'";
        String[] headers = {"book_id", "book_title", "borrow_date"};
        String[] columns = {"book_id", "title", "borrow_date"};
        db.viewRecords(query, headers, columns);
    }

    // Add a new book
    public static void addBook(config db, String title, String author, String date, String genre) {
        String sql = "INSERT INTO tbl_Librarian (title, names, date, genre) VALUES ('" +
                title + "', '" + author + "', '" + date + "', '" + genre + "')";
        db.addRecord(sql);
    }

    // Edit existing book
    public static void editBook(config db, int id, String title, String author, String date, String genre) {
        String sql = "UPDATE tbl_Librarian SET title='" + title + "', names='" + author + 
                     "', date='" + date + "', genre='" + genre + "' WHERE bid=" + id;
        db.updateRecord(sql);
    }

    // Delete book
    public static void deleteBook(config db, int id) {
        String sql = "DELETE FROM tbl_Librarian WHERE bid=" + id;
        db.deleteRecord(sql);
    }

    // Approve user
    public static void approveUser(config db, String email) {
        String sql = "UPDATE tbl_log SET u_status='Approved' WHERE u_email='" + email + "'";
        db.updateRecord(sql);
    }

    // Process book return
    public static void processReturn(config db, int bookId, int lateDays, String returnDate) {
        String findSql = "SELECT borrower_email FROM tbl_borrowed WHERE book_id=" + bookId + " AND status='Borrowed'";
        List<Map<String, Object>> info = db.fetchRecords(findSql);

        if (!info.isEmpty()) {
            String email = info.get(0).get("borrower_email").toString();

            String nameSql = "SELECT b_name FROM tbl_log WHERE u_email='" + email + "'";
            List<Map<String, Object>> nameInfo = db.fetchRecords(nameSql);
            String borrowerName = nameInfo.isEmpty() ? "Unknown" : nameInfo.get(0).get("b_name").toString();

            String updateSql = "UPDATE tbl_borrowed SET status='Returned', return_date='" + returnDate +
                    "' WHERE borrower_email='" + email + "' AND book_id=" + bookId;
            db.updateRecord(updateSql);

            if (lateDays > 0) {
                double penalty = lateDays * 10;
                // ✅ FIXED SQL HERE (previously incomplete)
                String penaltySql = "INSERT INTO tbl_penalty (b_name, b_email, b_id, b_days, b_penal, b_dpenal) " +
                        "VALUES ('" + borrowerName + "', '" + email + "', " + bookId + ", " +
                        lateDays + ", " + penalty + ", datetime('now'))";
                db.addRecord(penaltySql);
                System.out.println("Penalty added: P" + penalty + " for " + lateDays + " late days");
            } else {
                System.out.println("No penalty - returned on time!");
            }
        } else {
            System.out.println("Book not currently borrowed!");
        }
    }

    // Borrow a book
    public static void borrowBook(config db, String email, int bookId, String borrowDate) {
        String checkSql = "SELECT * FROM tbl_borrowed WHERE borrower_email='" + email +
                "' AND book_id=" + bookId + " AND status='Borrowed'";
        List<Map<String, Object>> already = db.fetchRecords(checkSql);

        if (already.isEmpty()) {
            String sql = "INSERT INTO tbl_borrowed (borrower_email, book_id, status, borrow_date) VALUES ('" +
                    email + "', " + bookId + ", 'Borrowed', '" + borrowDate + "')";
            db.addRecord(sql);
            System.out.println("Book borrowed!");
        } else {
            System.out.println("You already have this book!");
        }
    }

    // Main program
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        config db = new config();

        while (true) {
            System.out.println("\n=== Library System ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choice: ");
            int ch = sc.nextInt();
            sc.nextLine();

            if (ch == 3) {
                System.out.println("Goodbye!");
                break;
            }

            if (ch == 1) {
                System.out.print("Email: ");
                String email = sc.nextLine();
                System.out.print("Password: ");
                String password = sc.nextLine();

                String hashedPass = config.hashPassword(password);
                if (hashedPass == null) {
                    System.out.println("Hashing error!");
                    continue;
                }

                String loginSql = "SELECT * FROM tbl_log WHERE u_email='" + email +
                        "' AND u_pass='" + hashedPass + "' AND u_status='Approved'";
                List<Map<String, Object>> result = db.fetchRecords(loginSql);

                if (result.isEmpty()) {
                    System.out.println("Login failed!");
                    continue;
                }

                String userType = result.get(0).get("u_type").toString();
                System.out.println("Welcome " + userType + "!");

                if (userType.equalsIgnoreCase("Admin")) {
                    System.out.println("1. Approve User");
                    System.out.println("2. View Penalties");
                    System.out.print("Choice: ");
                    int choice = sc.nextInt();
                    sc.nextLine();

                    if (choice == 1) {
                        viewUsers(db);
                        System.out.print("Email to approve: ");
                        String userEmail = sc.nextLine();
                        approveUser(db, userEmail);
                        System.out.println("Approved!");
                    } else if (choice == 2) {
                        viewPenalties(db);
                    }

                } else if (userType.equalsIgnoreCase("Librarian")) {
                    System.out.println("1. Add Book");
                    System.out.println("2. Edit Book");
                    System.out.println("3. View Books");
                    System.out.println("4. Delete Book");
                    System.out.println("5. Process Return");
                    System.out.print("Choice: ");
                    int choice = sc.nextInt();
                    sc.nextLine();

                    if (choice == 1) {
                        System.out.print("Title: ");
                        String title = sc.nextLine();
                        System.out.print("Author: ");
                        String author = sc.nextLine();
                        System.out.print("Date (YYYY-MM-DD): ");
                        String date = sc.nextLine();
                        System.out.print("Genre: ");
                        String genre = sc.nextLine();
                        addBook(db, title, author, date, genre);
                    } else if (choice == 2) {
                        viewbook(db);
                        System.out.print("Book ID: ");
                        int id = sc.nextInt();
                        sc.nextLine();
                        System.out.print("New Title: ");
                        String title = sc.nextLine();
                        System.out.print("New Author: ");
                        String author = sc.nextLine();
                        System.out.print("New Date: ");
                        String date = sc.nextLine();
                        System.out.print("New Genre: ");
                        String genre = sc.nextLine();
                        editBook(db, id, title, author, date, genre);
                    } else if (choice == 3) {
                        viewbook(db);
                    } else if (choice == 4) {
                        viewbook(db);
                        System.out.print("Book ID: ");
                        int id = sc.nextInt();
                        sc.nextLine();
                        deleteBook(db, id);
                    } else if (choice == 5) {
                        System.out.println("\n--- ALL BORROWED BOOKS ---");
                        viewAllBorrowedBooksWithDetails(db);
                        System.out.print("Book ID to return: ");
                        int bookId = sc.nextInt();
                        sc.nextLine();
                        System.out.print("Days Late (0 if on time): ");
                        int lateDays = sc.nextInt();
                        sc.nextLine();
                        System.out.print("Return Date (YYYY-MM-DD): ");
                        String returnDate = sc.nextLine();
                        processReturn(db, bookId, lateDays, returnDate);
                    }

                } else {
                    // Borrower
                    System.out.println("1. View Books");
                    System.out.println("2. My Books");
                    System.out.println("3. Borrow Book");
                    System.out.print("Choice: ");
                    int choice = sc.nextInt();
                    sc.nextLine();

                    if (choice == 1) {
                        viewbook(db);
                    } else if (choice == 2) {
                        System.out.println("\n--- YOUR BORROWED BOOKS ---");
                        viewBorrowedBooks(db, email);
                    } else if (choice == 3) {
                        viewbook(db);
                        System.out.print("Book ID: ");
                        int bookId = sc.nextInt();
                        sc.nextLine();
                        System.out.print("Borrow Date (YYYY-MM-DD): ");
                        String borrowDate = sc.nextLine();
                        borrowBook(db, email, bookId, borrowDate);
                    }
                }
            } else if (ch == 2) {
                System.out.print("Name: ");
                String name = sc.nextLine();
                System.out.print("Email: ");
                String email = sc.nextLine();
                System.out.print("Password: ");
                String pass = sc.nextLine();

                String checkSql = "SELECT * FROM tbl_log WHERE u_email='" + email + "'";
                List<Map<String, Object>> exists = db.fetchRecords(checkSql);

                if (!exists.isEmpty()) {
                    System.out.println("Email exists!");
                    continue;
                }

                String hashedPass = config.hashPassword(pass);
                if (hashedPass == null) {
                    System.out.println("Hashing error!");
                    continue;
                }

                String insertSql = "INSERT INTO tbl_log (b_name, u_email, u_pass, u_type, u_status) VALUES ('" +
                        name + "', '" + email + "', '" + hashedPass + "', 'Borrower', 'Pending')";
                db.addRecord(insertSql);
                System.out.println("Registered! Wait for approval.");
            }
        }

        sc.close();
    }
}
