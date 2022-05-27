package lk.ijse.dep8.tasks.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOTest {

    private Connection connection;
    @BeforeEach
    void setUpConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");

       connection= DriverManager.getConnection("jdbc:mysql://localhost:3306/dep8_tasks","root","root");

    }

    @Test
    void existsUser() {
        boolean b = UserDAO.existsUser(connection, "abc2@gmail.com");
        assertTrue(b);

    }
}