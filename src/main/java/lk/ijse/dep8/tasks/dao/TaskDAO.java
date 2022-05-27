package lk.ijse.dep8.tasks.dao;


import java.sql.Connection;

public class TaskDAO {

    public static boolean existsTask(Connection connection, String taskId){return true;}
    public static void saveTask(Connection connection,TaskDAO taskDAO){}
    public static void updateTask(Connection connection,TaskDAO taskDAO){}
    public static void deleteTask(Connection connection,String taskId){}
    public static TaskDAO getTask(Connection connection,String taskId){return null;}

}
