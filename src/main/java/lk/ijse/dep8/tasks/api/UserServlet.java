package lk.ijse.dep8.tasks.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import lk.ijse.dep8.tasks.dto.UserDTO;
import org.apache.commons.codec.digest.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "UserServlet")
public class UserServlet extends HttpServlet2 {

    private final Logger logger = Logger.getLogger(UserServlet.class.getName());

    @Resource(name = "java:comp/env/jdbc/pool")
    private volatile DataSource pool;

    private UserDTO getUser(HttpServletRequest req) {
        if (!(req.getPathInfo() != null &&
                (req.getPathInfo().replaceAll("/", "").length() == 36))) {
            throw new ResponseStatusException(404, "Invalid user id");
        }

        String userId = req.getPathInfo().replaceAll("/", "");

        try (Connection connection = pool.getConnection()) {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM user WHERE id=?");
            stm.setString(1, userId);
            ResultSet rst = stm.executeQuery();

            if (!rst.next()) {
                throw new ResponseStatusException(404, "Invalid user id");
            } else {
                String name = rst.getString("full_name");
                String email = rst.getString("email");
                String password = rst.getString("password");
                String picture = rst.getString("profile_pic");
                return new UserDTO(userId, name, email, password, picture);
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(500, "Failed to fetch the user info", e);
        }
    }

    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        if (request.getContentType() == null || !request.getContentType().startsWith("multipart/form-data")) {
            throw new ResponseStatusException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Invalid content type or no content type is provided");
        }

        UserDTO user = getUser(request);

        String name = request.getParameter("name");
        String password = request.getParameter("password");
        Part picture = request.getPart("picture");

        if (name == null || !name.matches("[A-Za-z ]+")) {
            throw new ResponseStatusException(HttpServletResponse.SC_BAD_REQUEST, "Invalid name or name is empty");
        } else if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpServletResponse.SC_BAD_REQUEST, "Password can't be empty");
        } else if (picture != null && (picture.getSize() == 0 || !picture.getContentType().startsWith("image"))) {
            throw new ResponseStatusException(HttpServletResponse.SC_BAD_REQUEST, "Invalid picture");
        }

        Connection connection = null;
        try {
            connection = pool.getConnection();
            connection.setAutoCommit(false);

            PreparedStatement stm = connection.
                    prepareStatement("UPDATE user SET full_name=?, password=?, profile_pic=? WHERE id=?");
            stm.setString(1, name);
            stm.setString(2, DigestUtils.sha256Hex(password));

            String pictureUrl = null;
            if (picture != null) {
                pictureUrl = request.getScheme() + "://" + request.getServerName() + ":"
                        + request.getServerPort() + request.getContextPath();
                pictureUrl += "/uploads/" + user.getId();
            }
            stm.setString(3, pictureUrl);
            stm.setString(4, user.getId());

            if (stm.executeUpdate() != 1) {
                throw new SQLException("Failed to update the user");
            }

            String appLocation = getServletContext().getRealPath("/");
            Path path = Paths.get(appLocation, "uploads");
            Path picturePath = path.resolve(user.getId());

            if (picture != null) {
                if (Files.notExists(path)) {
                    Files.createDirectory(path);
                }

                Files.deleteIfExists(picturePath);
                picture.write(picturePath.toAbsolutePath().toString());
            } else {
                Files.deleteIfExists(picturePath);
            }

            connection.commit();
            resp.setStatus(204);
        } catch (SQLException e) {
            throw new ResponseStatusException(500, e.getMessage(), e);
        } finally {
            try {
                if (connection != null) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserDTO user = getUser(req);
        try (Connection connection = pool.getConnection()) {
            PreparedStatement stm = connection.prepareStatement("DELETE FROM user WHERE id=?");
            stm.setString(1, user.getId());
            if (stm.executeUpdate() != 1) {
                throw new SQLException("Failed to delete the user");
            }
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

            new Thread(() -> {
                Path imagePath = Paths.get(getServletContext().getRealPath("/"), "uploads",
                        user.getId());
                try {
                    Files.deleteIfExists(imagePath);
                } catch (IOException e) {
                    logger.warning("Failed to delete the image: " + imagePath.toAbsolutePath());
                }
            }).start();
        } catch (SQLException e) {
            throw new ResponseStatusException(500, e.getMessage(), e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserDTO user = getUser(req);
        Jsonb jsonb = JsonbBuilder.create();
        resp.setContentType("application/json");
        jsonb.toJson(user, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (request.getContentType() == null || !request.getContentType().startsWith("multipart/form-data")) {
            throw new ResponseStatusException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Invalid content type or no content type is provided");
        }

        if (request.getPathInfo() != null && !request.getPathInfo().equals("/")) {
            throw new ResponseStatusException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Invalid end point for a POST request");
        }

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        Part picture = request.getPart("picture");

        if (name == null || !name.matches("[A-Za-z ]+")) {
            throw new ResponseStatusException(HttpServletResponse.SC_BAD_REQUEST, "Invalid name or name is empty");
        } else if (email == null || !email.matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")) {
            throw new ResponseStatusException(HttpServletResponse.SC_BAD_REQUEST, "Invalid email or email is empty");
        } else if (password == null || password.trim().isEmpty()) {
            throw new ResponseStatusException(HttpServletResponse.SC_BAD_REQUEST, "Password can't be empty");
        } else if (picture != null && (picture.getSize() == 0 || !picture.getContentType().startsWith("image"))) {
            throw new ResponseStatusException(HttpServletResponse.SC_BAD_REQUEST, "Invalid picture");
        }

        Connection connection = null;
        try {
            connection = pool.getConnection();

            PreparedStatement stm = connection.prepareStatement("SELECT id FROM user WHERE email = ?");
            stm.setString(1, email);
            if (stm.executeQuery().next()) {
                throw new ResponseStatusException(HttpServletResponse.SC_CONFLICT, "A user has been already registered with this email");
            }

            connection.setAutoCommit(false);

            stm = connection.
                    prepareStatement("INSERT INTO user (id, email, password, full_name, profile_pic) VALUES (?, ?, ?, ?, ?)");
            String id = UUID.randomUUID().toString();
            stm.setString(1, id);
            stm.setString(2, email);
            stm.setString(3, DigestUtils.sha256Hex(password));
            stm.setString(4, name);

            String pictureUrl = null;
            if (picture != null) {
                pictureUrl = request.getScheme() + "://" + request.getServerName() + ":"
                        + request.getServerPort() + request.getContextPath();
                pictureUrl += "/uploads/" + id;
            }
            stm.setString(5, pictureUrl);

            if (stm.executeUpdate() != 1) {
                throw new SQLException("Failed to register the user");
            }

            if (picture != null) {
                String appLocation = getServletContext().getRealPath("/");
                Path path = Paths.get(appLocation, "uploads");
                if (Files.notExists(path)) {
                    Files.createDirectory(path);
                }

                String picturePath = path.resolve(id).toAbsolutePath().toString();
                picture.write(picturePath);
            }

            connection.commit();

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("application/json");
            UserDTO userDTO = new UserDTO(id, name, email, password, pictureUrl);
            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(userDTO, response.getWriter());
        } catch (SQLException e) {
            throw new ResponseStatusException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register the user", e);
        } finally {
            try {
                if (connection != null) {
                    connection.rollback();
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }


}
