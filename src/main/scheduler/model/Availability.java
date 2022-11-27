package scheduler.model;

import scheduler.db.ConnectionManager;
import java.sql.*;

public class Availability {
    private final Date date;
    private final String caregiver;

    private Availability(AvailabilityGetter getter){
        this.date = getter.date;
        this.caregiver = getter.caregiver;
    }

    public Date getDate () { return date; }

    public String getCaregiver() { return caregiver; }

    public void deleteAvailability () throws SQLException  {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String delete = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(delete);
            statement.setDate(1, date);
            statement.setString(2, caregiver);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AvailabilityGetter {
        private final Date date;
        private String caregiver;

        public AvailabilityGetter(Date date) {
            this.date = date;
        }

        public Availability get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAvailability = "SELECT Time, Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
            try {
                PreparedStatement statement = con.prepareStatement(getAvailability);
                statement.setDate(1, this.date);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    this.caregiver = resultSet.getString("Username");
                    return new Availability(this);
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
