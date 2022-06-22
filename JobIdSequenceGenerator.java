package eccm.dto;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

public class JobIdSequenceGenerator implements IdentifierGenerator {

    public Serializable generate(SessionImplementor session, Object collection) throws HibernateException {
        Connection connection = session.connection();
        PreparedStatement ps = null;
        String result = "";

        try {
            // Query a sequence
            ps = connection.prepareStatement("SELECT nextval('eccm.c_job_job_id_seq') AS TABLE_PK");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int pk = rs.getInt("TABLE_PK");

                // Convert to a String
                result = Integer.toString(pk);
            }
        } catch (SQLException e) {
            throw new HibernateException("Unable to generate Primary Key");
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    throw new HibernateException("Unable to close prepared statement.");
                }
            }
        }

        return result;
    }
}