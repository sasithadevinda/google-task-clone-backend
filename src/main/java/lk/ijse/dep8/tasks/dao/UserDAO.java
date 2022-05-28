package lk.ijse.dep8.tasks.dao;

import lk.ijse.dep8.tasks.dto.UserDTO;
import org.apache.commons.codec.digest.DigestUtils;

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
    public static void saveUser(Connection connection,UserDTO userDTO){
        try {
            PreparedStatement stm = connection.
                    prepareStatement("INSERT INTO user (id, email, password, full_name, profile_pic) VALUES (?, ?, ?, ?, ?)");
            stm.setString(1, userDTO.getId());
            stm.setString(2, userDTO.getEmail());
            stm.setString(3, userDTO.getPassword());
            stm.setString(4, userDTO.getName());
            stm.setString(5, userDTO.getPicture());

            if (stm.executeUpdate() != 1) {
                throw new SQLException("Failed to register the user");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static void updateUser(Connection connection,UserDTO userDTO){}
    public static void deleteUser(Connection connection,String userId){}
    public static UserDTO getUser(Connection connection,String userId){return null;}
}
