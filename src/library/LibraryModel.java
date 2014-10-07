package library;
/*
 * LibraryModel.java
 * Author:
 * Created on:
 */



import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class LibraryModel {

	// For use in creating dialogs and making them modal
	private JFrame dialogParent;
	private Connection con;

	public LibraryModel(JFrame parent, String userid, String password) {
		dialogParent = parent;
		try {
			Class.forName("org.postgresql.Driver");
			con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/pettandr_jdbc", "andrew", "123456");
		} catch (SQLException | ClassNotFoundException e) {
			showExceptionDialog(e);
			exit();
		}
	}

	public String bookLookup(int isbn) {
		ResultSet r = null;
		try {
			r = query(String.format("SELECT * FROM book WHERE isbn = '%d'", isbn));
			StringBuilder out = new StringBuilder("Book lookup:\n");
			while(r.next()) {
				out.append(String.format("%d: %s (Edition %d)\nCopies: %d (%d left)\n",
						r.getInt("isbn"), r.getString("title"), r.getInt("edition_no"), r.getInt("numofcop"), r.getInt("numleft")));
				
				//Get authors
				ResultSet ar = query(String.format("SELECT a.name, a.surname FROM book_author b, author a WHERE b.isbn = '%d' AND a.authorid = b.authorid", isbn));
				StringBuilder authors = new StringBuilder("Author/s: ");
				
				boolean auth = false; //Any authors?
				while(ar.next()) {
					authors.append(String.format("%s%s", (auth ? ", " : ""), ar.getString("surname").trim()));
					auth = true;
				}
				
				if(auth)
					out.append(authors);
				else
					out.append("No authors.");
			}

			return out.toString();
		} catch (SQLException e) {
			showExceptionDialog(String.format("There was an error executing the query: %s", e.toString()));
			return "Database error.";
		}
	}

	public String showCatalogue() {
		return "Show Catalogue Stub";
	}

	public String showLoanedBooks() {
		return "Show Loaned Books Stub";
	}

	public String showAuthor(int authorID) {
		return "Show Author Stub";
	}

	public String showAllAuthors() {
		return "Show All Authors Stub";
	}

	public String showCustomer(int customerID) {
		return "Show Customer Stub";
	}

	public String showAllCustomers() {
		return "Show All Customers Stub";
	}

	public String borrowBook(int isbn, int customerID,
			int day, int month, int year) {
		return "Borrow Book Stub";
	}

	public String returnBook(int isbn, int customerid) {
		return "Return Book Stub";
	}

	public void closeDBConnection() {
		try {
			con.close();
		} catch (SQLException e) {
			showExceptionDialog(e);
		}
	}

	public String deleteCus(int customerID) {
		return "Delete Customer";
	}

	public String deleteAuthor(int authorID) {
		return "Delete Author";
	}

	public String deleteBook(int isbn) {
		return "Delete Book";
	}
	
	private void showExceptionDialog(Exception e) {
		showExceptionDialog(e.toString());
	}
	
	private void showExceptionDialog(String s) {
		showMessageDialog(dialogParent,
				s,
				"Error performing action",
				ERROR_MESSAGE);
	}
	
	private void showNoticeDialog(String s) {
		showMessageDialog(dialogParent,
				s,
				"Error performing action",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	private ResultSet query(String query) throws SQLException {
		Statement st = con.createStatement();
		return st.executeQuery(query);
	}
	
	private int update(String query) throws SQLException {
		Statement st = con.createStatement();
		return st.executeUpdate(query);
	}
	
	private void exit() {
		System.exit(0);
	}
}