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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

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
			con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		} catch (SQLException | ClassNotFoundException e) {
			showExceptionDialog(e);
			exit();
		}
	}

	public String bookLookup(int isbn) {
		ResultSet r = null;
		try {
			r = query(String.format("SELECT book.isbn, title, edition_no, numofcop, numleft, string_agg(author.surname, ', ') as authors " +
					"FROM book " +
					"LEFT OUTER JOIN book_author ON (book.isbn = book_author.isbn) " +
					"LEFT OUTER JOIN author ON (book_author.authorid = author.authorid) " +
					"WHERE book.isbn = %d " +
					"GROUP BY book.isbn " +
					"ORDER BY book.isbn", isbn));
			r.next();
			return bookLookupFormat(r);
			
		} catch (SQLException e) {
			showExceptionDialog(String.format("There was an error executing the query: %s", e.getMessage()));
			return databaseError();
		}
	}
	
	private String bookLookupFormat(ResultSet r) throws SQLException {
		StringBuilder out = new StringBuilder("Book lookup:\n");
		out.append(String.format("\t%d: %s\n\tEdition %d - Copies: %d (%d left)\n\t",
				r.getInt("isbn"), r.getString("title").trim(), r.getInt("edition_no"), r.getInt("numofcop"), r.getInt("numleft")));
		
		String authors = r.getString("authors");
		out.append(authors != null && authors.trim().length() > 0 ? "Authors: " + authors : "No authors.");

		return out.toString();
	}

	public String showCatalogue() {
		ResultSet r = null;
		try {
			r = query(String.format("SELECT book.isbn, title, edition_no, numofcop, numleft, string_agg(author.surname, ', ') as authors " +
					"FROM book " +
					"LEFT OUTER JOIN book_author ON (book.isbn = book_author.isbn) " +
					"LEFT OUTER JOIN author ON (book_author.authorid = author.authorid) " +
					"GROUP BY book.isbn " +
					"ORDER BY book.isbn"));
			StringBuilder out = new StringBuilder("Catalogue:\n");
			while(r.next()) {
				out.append(String.format("%s\n\n", bookLookupFormat(r)));
			}
			return out.toString();
		} catch (SQLException e) {
			showExceptionDialog(String.format("There was an error executing the query: %s", e.getMessage()));
			return databaseError();
		}
	}

	public String showLoanedBooks() {
		return "stub";
	}

	public String showAuthor(int authorID) {
		ResultSet r = null;
		try {
			r = query(String.format("SELECT author.authorid, author.name, author.surname, string_agg(book.isbn::varchar || ': ' || book.title || ' (Ed. ' || book.edition_no || ')', ', ') as books " +
					"FROM author " +
					"LEFT OUTER JOIN book_author ON (book_author.authorid = author.authorid) " +
					"LEFT OUTER JOIN book ON (book.isbn = book_author.isbn) " +
					"WHERE author.authorid = %d" +
					"GROUP BY author.authorid", authorID));
			StringBuilder out = new StringBuilder("Show Author:\n");
			boolean any = printAuthors(r, out);
			if(!any) {
				showNoticeDialog("No author found with that ID");
				return "";
			}
			return out.toString();
		} catch (SQLException e) {
			showExceptionDialog(String.format("There was an error executing the query: %s", e.getMessage()));
			return databaseError();
		}
	}
	
	private boolean printAuthors(ResultSet r, StringBuilder out) throws SQLException {
		boolean any = false;
		while(r.next()) {
			any = true;
			out.append(String.format("\t%d - %s %s\n\tBooks written:\n", 
					r.getInt("authorid"), r.getString("name").trim(), r.getString("surname").trim()));
			String books = r.getString("books");
			if(books != null) {
				String[] booksarr = books.split(", ");
				for(String s : booksarr) {
					out.append("\t\t" + s + "\n");
				}
			} else {
				out.append("\t\tNone\n");
			}
			out.append("\n");
		}
		return any;
	}

	public String showAllAuthors() {
		ResultSet r = null;
		try {
			r = query("SELECT author.authorid, author.name, author.surname, string_agg(book.isbn::varchar || ': ' || book.title || ' (Ed. ' || book.edition_no || ')', ', ') as books " +
					"FROM author " +
					"LEFT OUTER JOIN book_author ON (book_author.authorid = author.authorid) " +
					"LEFT OUTER JOIN book ON (book.isbn = book_author.isbn) " +
					"GROUP BY author.authorid " +
					"ORDER BY author.authorid");
			StringBuilder out = new StringBuilder("Show All Authors:\n");
			boolean any = printAuthors(r, out);
			if(!any) {
				showNoticeDialog("There are no authors in the database.");
				return "";
			}
			
			return out.toString();
		} catch (SQLException e) {
			showExceptionDialog(String.format("There was an error executing the query: %s", e.getMessage()));
			return databaseError();
		}
	}

	public String showCustomer(int customerID) {
		ResultSet r = null;
		try {
			r = query(String.format("SELECT customer.customerid, customer.l_name, customer.f_name, customer.city, string_agg(book.isbn || ' - f[Due: ' || cust_book.duedate || ']' || ' - ' || book.title || ' (Ed. ' || book.edition_no || ')', ', ') as loaned_books " +
					"FROM customer " +
					"LEFT OUTER JOIN cust_book ON (cust_book.customerid = customer.customerid) " +
					"LEFT OUTER JOIN book ON (cust_book.isbn = book.isbn) " +
					"WHERE customer.customerid = %d" +
					"GROUP BY customer.customerid " +
					"ORDER BY customer.customerid", customerID));
			StringBuilder out = new StringBuilder("Show Customer:\n");
			boolean any = printCustomers(r, out, true);
			if(!any) {
				showNoticeDialog("No customer with given ID");
				return "";
			}
			return out.toString();
		} catch (SQLException e) {
			showExceptionDialog(String.format("There was an error executing the query: %s", e.getMessage()));
			return databaseError();
		}
	}
	
	private boolean printCustomers(ResultSet r, StringBuilder out, boolean showBooks) throws SQLException {
		boolean any = false;
		while(r.next()) {
			any = true;
			out.append(String.format("\t%d: %s, %s - %s\n", 
					r.getInt("customerid"), r.getString("l_name").trim(), trimOrNull(r.getString("f_name"), ""), trimOrNull(r.getString("city"), "(no city)")));
			if(showBooks) {
				out.append("\tBooks borrowed:\n");
				String loaned_books = r.getString("loaned_books");
				if(loaned_books != null) {
					String[] lbarr = loaned_books.split(", ");
					for(String s : lbarr) {
						out.append("\t\t" + s + "\n");
					}
				} else {
					out.append("\t\tNone.\n");
				}
			}
		}
		return any;
	}

	public String showAllCustomers() {
		ResultSet r = null;
		try {
			r = query(String.format("SELECT customer.customerid, customer.l_name, customer.f_name, customer.city " +
					"FROM customer " +
					"ORDER BY customer.customerid"));
			StringBuilder out = new StringBuilder("Show Customer:\n");
			boolean any = printCustomers(r, out, false);
			if(!any) {
				showNoticeDialog("No customers found");
				return "";
			}
			return out.toString();
		} catch (SQLException e) {
			showExceptionDialog(String.format("There was an error executing the query: %s", e.toString()));
			return databaseError();
		}
	}

	public String borrowBook(int isbn, int customerID, int day, int month, int year) {
		try {
			try {
				con.setAutoCommit(false);
				ResultSet customer = query(String.format("SELECT * FROM customer WHERE customerid = %d FOR UPDATE", customerID));
				if(!customer.next())
					throw new RuntimeException("Customer does not exist");
				
				ResultSet book = query(String.format("SELECT * FROM book WHERE isbn = %d FOR UPDATE", isbn));
				if(!book.next())
					throw new RuntimeException("Book does not exist");
				
				int numcopies = book.getInt("numleft");
				if(numcopies <= 0)
					throw new RuntimeException("There are no copies left of this book");
				
				PreparedStatement cust_book = con.prepareStatement("INSERT INTO cust_book VALUES(?, ?, ?)");
				cust_book.setInt(1, isbn);
				java.sql.Date date = java.sql.Date.valueOf(String.format("%d-%s-%s", year, month >= 10 ? String.valueOf(month) : "0" + month,
						day >= 10 ? String.valueOf(day) : "0" + day));
				cust_book.setDate(2, date);
				cust_book.setInt(3, customerID);
				
				int ret = cust_book.executeUpdate();
				
				showNoticeDialog("The customer and book records have been locked. Hit OK to proceed.");
				
				int ret2 = update(String.format("UPDATE book SET numleft = numleft - 1 WHERE isbn = %d", isbn));
				
				con.commit();
				
				return String.format("Borrow Book:\n\tBook: %d (%s)\n\tLoaned to: %d (%s, %s)\n\tDue Date: %s",
						isbn, trimOrNull(book.getString("title"), ""), customerID, trimOrNull(customer.getString("l_name"), ""), trimOrNull(customer.getString("f_name"), ""), date.toString());
				
				
			} catch(RuntimeException e) {
				con.rollback();
				showExceptionDialog(e.getMessage());
				return "Borrow book:\n\t" + e.getMessage();
			} catch(SQLException e) {
				con.rollback();
				throw e;
			} finally {
				con.setAutoCommit(true);
			}
		} catch(SQLException e) {
			if(e.getMessage().replaceAll("\\s+", "").matches("(.*)cust_book_pkey(.*)")) {
				showExceptionDialog("That customer already has this book on loan.");
				return "Borrow book:\n\tThat customer already has this book on loan.";
			}
			else {
				showExceptionDialog("There has been a database error. All actions have been reversed.");
				System.out.println("===========================\nDatabase error:\n"+e.getMessage()+"\n===========================");
				return "Borrow book:\n\tThere has been a database error. All actions have been reversed.";
			}
		}
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
		try {
			ResultSet r = query(String.format("DELETE FROM customer WHERE customerid = %d", customerID));
			return "Customer deleted.";
		} catch(SQLException e) {
			showExceptionDialog(String.format("Could not delete the customer: %s", e.getMessage()));
			return "";
		}
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
	
	private String trimOrNull(String str, String defau) {
		if(str == null)
			return defau;
		else return str.trim();
	}
	
	private String databaseError() {
		return String.format("====================\nThere was a database error.\nNo actions have been committed.\n====================");
	}
}