package lk.ijse.dep8.tasks.dao;

import lk.ijse.dep8.tasks.dto.UserDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public static boolean existsUser(Connection connection, String userId){
        try {
            PreparedStatement stm = connection.prepareStatement("SELECT *FROM user WHERE email=?");
        stm.setString(1,userId);
            ResultSet rst = stm.executeQuery();
            return rst.next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


       }
    public static void saveUser(Connection connection,UserDTO userDTO){}
    public static void updateUser(Connection connection,UserDTO userDTO){}
    public static void deleteUser(Connection connection,String userId){}
    public static UserDTO getUser(Connection connection,String userId){return null;}
}
