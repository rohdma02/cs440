package mlb;

/**
 * @author Roman Yasinovskyy
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseReader {

    private Connection db_connection;
    private final String SQLITEDBPATH = "jdbc:sqlite:data/mlb/mlb.sqlite";

    public DatabaseReader() {
    }

    /**
     * Connect to a database (file)
     */
    public void connect() {
        try {
            this.db_connection = DriverManager.getConnection(SQLITEDBPATH);
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseReaderGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Disconnect from a database (file)
     */
    public void disconnect() {
        try {
            this.db_connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseReaderGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Populate the list of divisions
     *
     * @param divisions
     */
    public void getDivisions(ArrayList<String> divisions) {
        Statement stat;
        ResultSet results;

        this.connect();
        try {
            stat = this.db_connection.createStatement();
            // TODO: Write an SQL statement to retrieve a league (conference) and a division
            String sql = "select distinct(conference), division from team";
            
            results = stat.executeQuery(sql);
            
            // TODO: Add all 6 combinations to the ArrayList divisions
            while (results.next()) {
                divisions.add(results.getString("conference") 
                + " | " + results.getString("division"));
            }
            
            
            
            results.close();
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseReader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            this.disconnect();
        }
    }

    /**
     * Read all teams from the database
     *
     * @param confDiv
     * @param teams
     */
    public void getTeams(String confDiv, ArrayList<String> teams) {
        Statement stat;
        ResultSet results;
        String conference = confDiv.split(" | ")[0];
        String division = confDiv.split(" | ")[2];

        this.connect();
        try {
            stat = this.db_connection.createStatement();
            // TODO: Write an SQL statement to retrieve a teams from a specific division
            String sql = "select name from team where conference = '" 
            + conference + "' and division = '" + division + "'";
            results = stat.executeQuery(sql);
            // TODO: Add all 5 teams to the ArrayList teams
            while (results.next()) {
                teams.add(results.getString("name"));   
            }
            results.close();
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseReader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            this.disconnect();
        }
    }

    /**
     * @param teamName
     * @return Team info
     */
    public Team getTeamInfo(String teamName) {
        Statement stat;
        ResultSet results;
        Team team = null;
        ArrayList<Player> roster = new ArrayList<>();
        Address address = null;
        // Retrieve team info (roster, address, and logo) from the database
        this.connect();
        try {
            stat = this.db_connection.createStatement();
            ResultSet team_from_team = stat.executeQuery("select idpk, id, abbr, name, conference, division, logo from team where name = '" + teamName + "'");
            int team_idpk = team_from_team.getInt("idpk");
            team = new Team(team_from_team.getString("id"), 
            team_from_team.getString("abbr"), 
            team_from_team.getString("name"),
            team_from_team.getString("conference"),
            team_from_team.getString("division"));
            team.setLogo(team_from_team.getBytes("logo"));


            ResultSet team_from_adress = stat.executeQuery("select team, site, street, city,state, zip, phone, url from address where team = '" + team_idpk + "'");
            // String team_name = team_from_adress.getString(1);
            address = new Address(
                team_from_adress.getString(team_idpk),
                team_from_adress.getString("site"), 
                team_from_adress.getString("street"), 
                team_from_adress.getString("city"),
                team_from_adress.getString("state"),
                team_from_adress.getString("zip"),
                team_from_adress.getString("phone"), 
                team_from_adress.getString("url"));

            ResultSet team_from_roster = stat.executeQuery("select id, name, team, position from player where team = '" + team_idpk + "'");
        
            while (team_from_roster.next()) {
                roster.add(new Player(team_from_roster.getString("id"), 
                team_from_roster.getString("name"), 
                team_from_roster.getString("team"), 
                team_from_roster.getString("position")));
            }

            team.setRoster(roster);
            team.setAddress(address);
            

       

            
            
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseReader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            this.disconnect();
        }
        return team;
        
    }

       
}
