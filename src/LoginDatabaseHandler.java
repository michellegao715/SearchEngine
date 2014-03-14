import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Part of the {@link LoginServer} example. Handles all database-related
 * actions.
 * 
 * @see LoginServer
 * @see DatabaseConnector2
 */
public class LoginDatabaseHandler {

	/** A logger for debugging. */
	private static final Logger log = LogManager.getLogger();

	/** Makes sure only one database handler is instantiated. */
	private static LoginDatabaseHandler singleton = new LoginDatabaseHandler();

	/** Used to determine if tables are already setup. */
	private static final String TABLES_SQL = "SHOW TABLES LIKE 'login_users';";

	/** Used to create necessary tables for this example. */
	private static final String CREATE_SQL = "CREATE TABLE login_users ("
			+ "userid INTEGER AUTO_INCREMENT PRIMARY KEY, "
			+ "username VARCHAR(32) NOT NULL UNIQUE, "
			+ "password CHAR(64) NOT NULL, " + "usersalt CHAR(32) NOT NULL);";

	/** Used to insert a new user into the database. */
	private static final String REGISTER_SQL = "INSERT INTO login_users (username, password, usersalt) "
			+ "VALUES (?, ?, ?);";

	private static final String HISTORY_SQL = "INSERT INTO login_users (username, query) "
			+ "VALUES (?, ?);";

	/** Used to determine if a username already exists. */
	private static final String USER_SQL = "SELECT username FROM login_users WHERE username = ?";

	/** Used to retrieve the salt associated with a specific user. */
	private static final String SALT_SQL = "SELECT usersalt FROM login_users WHERE username = ?";

	/** Used to authenticate a user. */
	private static final String AUTH_SQL = "SELECT username FROM login_users "
			+ "WHERE username = ? AND password = ?";

	/** Used to remove a user from the database. */
	private static final String DELETE_SQL = "DELETE FROM login_users WHERE username = ?";

	// TODO new added update.
	private static final String UPDATE_PASSWORD = "UPDATE login_users SET password = ? WHERE username = ?";

	private Status updatePassword(Connection connection, String newuser,
			String newpassword) throws SQLException {
		Status status = Status.ERROR;

		
		String usersalt = getSalt(connection, newuser);
		// save this hash in database.
		String passhash = StringUtilities.getHash(newpassword, usersalt);

		try (PreparedStatement statement = connection
				.prepareStatement(UPDATE_PASSWORD);) {
			statement.setString(1, passhash);
			statement.setString(2, newuser);
//			statement.setString(3, usersalt);
			statement.executeUpdate();
			status = Status.OK;
		}

		return status;
	}

	public Status updatePassword(String username, String oldpassword, String newpassword) {
		Status status = Status.ERROR;
		// make sure we have non-null and non-emtpy values for login
		if (StringUtilities.checkString(username)
				|| StringUtilities.checkString(oldpassword) ||(StringUtilities.checkString(newpassword))) {
			status = Status.INVALID_LOGIN;
			log.debug(status);
			return status;
		}
		log.debug("Change password of  " + username + ".");
		
		System.out.println("Change password of " + username + ".");

		try (Connection connection = db.getConnection();) {
			status = authenticateUser(connection, username, oldpassword);
			System.out.println("updatePassword: user authenticated");
			// if okay so far, try to insert new user
			if (status == Status.OK) {
				System.out.println("updatePassword: try change password");
				status = updatePassword(connection, username, newpassword);
			}
		} catch (SQLException ex) {
			status = Status.CONNECTION_FAILED;
			log.debug(status, ex);
		}

		return status;
	}

	/** Used to configure connection to database. */
	private DatabaseConnector db;

	/**
	 * Initializes a database handler for the Login example. Private constructor
	 * forces all other classes to use singleton.
	 */
	private LoginDatabaseHandler() {
		Status status = Status.OK;

		try {
			this.db = new DatabaseConnector();

			if (!db.testConnection()) {
				status = Status.CONNECTION_FAILED;
			} else {
				status = this.setupTables();
			}
		} catch (FileNotFoundException e) {
			status = Status.MISSING_CONFIG;
		} catch (IOException e) {
			status = Status.MISSING_VALUES;
		}

		// We cannot move on if the database handler fails, so exit
		if (status != Status.OK) {
			log.fatal(status.message());
			System.exit(-status.ordinal());
		}
	}

	/**
	 * Gets the single instance of the database handler.
	 * 
	 * @return instance of the database handler
	 */
	public static LoginDatabaseHandler getInstance() {
		return singleton;
	}

	/**
	 * Checks if necessary table exists in database, and if not tries to create
	 * it.
	 * 
	 * @return {@link Status.OK} if table exists or create is successful
	 */
	private Status setupTables() {
		Status status = Status.ERROR;
		ResultSet results = null;

		try (Connection connection = db.getConnection();
				Statement statement = connection.createStatement();) {
			// check if table exists in database
			statement.executeQuery(TABLES_SQL);
			results = statement.getResultSet();

			if (!results.next()) {
				log.debug("Creating tables...");

				// create table and check if successful
				statement.executeUpdate(CREATE_SQL);
				statement.executeQuery(TABLES_SQL);

				results = statement.getResultSet();
				status = (results.next()) ? Status.OK : Status.CREATE_FAILED;
			} else {
				log.debug("Tables found.");
				status = Status.OK;
			}
		} catch (Exception ex) {
			status = Status.CREATE_FAILED;
			log.debug(status, ex);
		}

		return status;
	}

	/**
	 * Tests if a user already exists in the database. Requires an active
	 * database connection.
	 * 
	 * @param connection
	 *            - active database connection
	 * @param user
	 *            - username to check
	 * @return Status.OK if user does not exist in database
	 * @throws SQLException
	 */
	private Status duplicateUser(Connection connection, String user)
			throws SQLException {
		Status status = Status.ERROR;

		try (PreparedStatement statement = connection
				.prepareStatement(USER_SQL);) {
			// plug in user data into ?, 1 means the replace the first "?" with
			// user.
			statement.setString(1, user);
			statement.executeQuery();

			ResultSet results = statement.getResultSet();
			status = results.next() ? Status.DUPLICATE_USER : Status.OK;
		}

		return status;
	}

	/**
	 * Tests if a user already exists in the database. !!Can just call this
	 * method to make sure it the username is already in database.
	 * 
	 * @see #duplicateUser(Connection, String)
	 * @param user
	 *            - username to check
	 * @return Status.OK if user does not exist in database
	 */
	public Status duplicateUser(String user) {
		Status status = Status.ERROR;

		try (Connection connection = db.getConnection();) {
			status = duplicateUser(connection, user);
		} catch (SQLException e) {
			status = Status.CONNECTION_FAILED;
			log.debug(e.getMessage(), e);
		}

		return status;
	}

	/**
	 * Registers a new user, placing the username, password hash, and salt into
	 * the database if the username does not already exist.
	 * 
	 * @param newuser
	 *            - username of new user
	 * @param newpass
	 *            - password of new user
	 * @return {@link Status.OK} if registration successful
	 * @throws SQLException
	 */

	/**
	 * Used to insert a new user into the database. private static final String
	 * REGISTER_SQL = "INSERT INTO login_users (username, password, usersalt) "
	 * + "VALUES (?, ?, ?);";
	 */

	private Status registerUser(Connection connection, String newuser,
			String newpass) throws SQLException {
		Status status = Status.ERROR;

		byte[] saltbyte = StringUtilities.randomBytes(16);
		String usersalt = StringUtilities.encodeHex(saltbyte, 32);
		// save this hash in database.
		String passhash = StringUtilities.getHash(newpass, usersalt);

		try (PreparedStatement statement = connection
				.prepareStatement(REGISTER_SQL);) {
			statement.setString(1, newuser);
			statement.setString(2, passhash);
			statement.setString(3, usersalt);
			statement.executeUpdate();
			status = Status.OK;
		}

		return status;
	}

	/**
	 * Registers a new user, placing the username, password hash, and salt into
	 * the database if the username does not already exist.
	 * 
	 * @param newuser
	 *            - username of new user
	 * @param newpass
	 *            - password of new user
	 * @return {@link Status.OK} if registration successful
	 */
	public Status registerUser(String newuser, String newpass) {
		Status status = Status.ERROR;

		log.debug("Registering " + newuser + ".");

		// make sure we have non-null and non-emtpy values for login
		if (StringUtilities.checkString(newuser)
				|| StringUtilities.checkString(newpass)) {
			status = Status.INVALID_LOGIN;
			log.debug(status);
			return status;
		}

		// try to connect to database and test for duplicate user
		try (Connection connection = db.getConnection();) {
			status = duplicateUser(connection, newuser);

			// if okay so far, try to insert new user
			if (status == Status.OK) {
				status = registerUser(connection, newuser, newpass);
			}
		} catch (SQLException ex) {
			status = Status.CONNECTION_FAILED;
			log.debug(status, ex);
		}

		return status;
	}

	/**
	 * Gets the salt for a specific user.
	 * 
	 * @param connection
	 *            - active database connection
	 * @param user
	 *            - which user to retrieve salt for
	 * @return salt for the specified user or null if user does not exist
	 * @throws SQLException
	 *             if any issues with database connection
	 */
	private String getSalt(Connection connection, String user)
			throws SQLException {
		String salt = null;

		try (PreparedStatement statement = connection
				.prepareStatement(SALT_SQL);) {
			statement.setString(1, user);
			statement.executeQuery();

			ResultSet results = statement.getResultSet();
			salt = results.next() ? results.getString("usersalt") : null;
		}

		return salt;
	}

	/**
	 * Checks if the provided username and password match what is stored in the
	 * database. Requires an active database connection.
	 * 
	 * @param username
	 *            - username to authenticate
	 * @param password
	 *            - password to authenticate
	 * @return {@link Status.OK} if authentication successful
	 * @throws SQLException
	 */
	private Status authenticateUser(Connection connection, String username,
			String password) throws SQLException {

		Status status = Status.ERROR;

		try (PreparedStatement statement = connection
				.prepareStatement(AUTH_SQL);) {
			String usersalt = getSalt(connection, username);
			String passhash = StringUtilities.getHash(password, usersalt);

			statement.setString(1, username);
			statement.setString(2, passhash);
			statement.executeQuery();

			ResultSet results = statement.getResultSet();
			status = results.next() ? status = Status.OK : Status.INVALID_LOGIN;
		}

		return status;
	}

	/**
	 * Checks if the provided username and password match what is stored in the
	 * database. Must retrieve the salt and hash the password to do the
	 * comparison.
	 * 
	 * @param username
	 *            - username to authenticate
	 * @param password
	 *            - password to authenticate
	 * @return {@link Status.OK} if authentication successful
	 */
	public Status authenticateUser(String username, String password) {
		Status status = Status.ERROR;

		log.debug("Authenticating user " + username + ".");

		try (Connection connection = db.getConnection();) {
			status = authenticateUser(connection, username, password);
		} catch (SQLException ex) {
			status = Status.CONNECTION_FAILED;
			log.debug(status, ex);
		}

		return status;
	}

	/**
	 * Removes a user from the database if the username and password are
	 * provided correctly.
	 * 
	 * @param username
	 *            - username to remove
	 * @param password
	 *            - password of user
	 * @return {@link Status.OK} if removal successful
	 * @throws SQLException
	 */
	private Status removeUser(Connection connection, String username,
			String password) throws SQLException {
		Status status = Status.ERROR;

		try (PreparedStatement statement = connection
				.prepareStatement(DELETE_SQL);) {
			statement.setString(1, username);
			int count = statement.executeUpdate();
			status = (count == 1) ? Status.OK : Status.INVALID_USER;
		}

		return status;
	}

	/**
	 * Removes a user from the database if the username and password are
	 * provided correctly.
	 * 
	 * @param username
	 *            - username to remove
	 * @param password
	 *            - password of user
	 * @return {@link Status.OK} if removal successful
	 */
	public Status removeUser(String username, String password) {
		Status status = Status.ERROR;

		log.debug("Removing user " + username + ".");

		try (Connection connection = db.getConnection();) {
			status = authenticateUser(connection, username, password);

			if (status == Status.OK) {
				status = removeUser(connection, username, password);
			}
		} catch (Exception ex) {
			status = Status.CONNECTION_FAILED;
			log.debug(status, ex);
		}

		return status;
	}
}