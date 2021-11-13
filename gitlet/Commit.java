package gitlet;
import java.io.Serializable;
import java.util.*;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Anthony Zhang
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private Date timeStamp;
    /** mapping for blobs */
    private TreeMap<String, String> allfiles;
    private String parentID1;
    private String parentID2;
    private String s1;

    //this makes the initial commit
    public Commit() {
        this.message = "initial commit";
        this.timeStamp = new Date(0);
        this.allfiles = new TreeMap<String, String>();
    }

    public Commit(String commitMsg) {
        this.message = commitMsg;
        this.timeStamp = new Date();
        String activeBranchName = readObject(Repository.HEADBRANCH, String.class);
        String sha1currentCommit = (String)
                readObject(Repository.BRANCHES, TreeMap.class).get(activeBranchName);
        Commit previousCommit =
                readObject(join(Repository.COMMITS, sha1currentCommit), Commit.class);
        this.parentID1 = sha1(serialize(previousCommit));

        //Sets up the mapping of files to blobs dataStruct in Commit obj
        this.allfiles = (TreeMap<String, String>)
                previousCommit.allfiles.clone();
        TreeMap<String, String> stagingareaaddTreemap =
                readObject(Repository.STAGING_AREA_ADD, TreeMap.class);
        TreeMap<String, String> stagingarearmTreemap =
                readObject(Repository.STAGING_AREA_RM, TreeMap.class);
        /** puts all files from staging area into commit obj */
        for (String key : stagingareaaddTreemap.keySet()) {
            this.allfiles.put(key, stagingareaaddTreemap.get(key));
        }
        /** removes all files that are staged for removal */
        for (String key : stagingarearmTreemap.keySet()) {
            this.allfiles.remove(key);
        }
        /** clears the adding and removal staging areas and saves them */
        stagingareaaddTreemap.clear();
        stagingarearmTreemap.clear();
        writeObject(Repository.STAGING_AREA_ADD, stagingareaaddTreemap);
        writeObject(Repository.STAGING_AREA_RM, stagingarearmTreemap);
        this.s1 = sha1(serialize(this));
    }

    public TreeMap<String, String> getAllfiles() {
        return this.allfiles;
    }

    public void setParent2(String p2) {
        this.parentID2 = p2;
        writeObject(join(Repository.COMMITS, sha1(serialize(this))), Commit.class);
    }

    public String getSha1() {
        return this.s1;
    }

    public String getMessage() {
        return this.message;
    }

    public Date getTimeStamp() {
        return this.timeStamp;
    }

    public String getParent1() {
        return this.parentID1;
    }

    public String getParent2() {
        return this.parentID2;
    }
}

