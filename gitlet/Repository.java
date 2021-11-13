package gitlet;

import java.io.File;
import java.util.*;
import java.text.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  This class is the way you interact with the repository to access a certain Commit.
 *
 *
 *  @author Anthony Zhang
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** The staging area, file for persistence */
    public static final File STAGING_AREA_ADD = join(GITLET_DIR, "staging_area_add");
    public static final File STAGING_AREA_RM = join(GITLET_DIR, "staging_area_rm");

    /** The Commits directory, contains all commit objects */
    public static final File COMMITS = join(GITLET_DIR, "Commits");

    /** what Commit the Head/Active branch points to*/
    public static final File HEADBRANCH = join(GITLET_DIR, "head");
    /** contains all the other branches that the user decides to create */
    public static final File BRANCHES = join(GITLET_DIR, "branches");
    /** this directory contains all the blobs */
    public static final File BLOBS = join(GITLET_DIR, "blobs");

    public static void initialCommit() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
            return;
        }
        GITLET_DIR.mkdir();
        COMMITS.mkdir();
        BLOBS.mkdir();
        Commit initialCommit = new Commit();
        File initialCommitFile = join(COMMITS, sha1(serialize(initialCommit)));
        writeObject(initialCommitFile, initialCommit);

        TreeMap<String, String> stagingareaadd1 = new TreeMap<>();
        writeObject(join(STAGING_AREA_ADD), stagingareaadd1);
        TreeMap<String, String> stagingarearm1 = new TreeMap<>();
        writeObject(join(STAGING_AREA_RM), stagingarearm1);
        TreeMap<String, String> branchesTM = new TreeMap<>();
        branchesTM.put("master", sha1(serialize(initialCommit)));
        writeObject(BRANCHES, branchesTM);
        writeObject(HEADBRANCH, "master");
    }

    public static void addThisToStagingArea(String filename) {
        if (!join(CWD, filename).exists()) {
            System.out.println("File does not exist.");
            return;
        }
        /** takes the contents of the CWD file
         * and puts it into a blob with BLOBS directory with name sha1(contents) */
        File blobToAdd = join(CWD, filename);
        File addedBlob = join(BLOBS, sha1(readContentsAsString(blobToAdd)));
        writeContents(addedBlob, readContentsAsString(blobToAdd));

        TreeMap<String, String> stagingareaadd = readObject(STAGING_AREA_ADD, TreeMap.class);
        TreeMap<String, String> stagingarearm = readObject(STAGING_AREA_RM, TreeMap.class);
        /** get the currentCommit that the headbranch is pointing to */
        String activeBranchName = readObject(HEADBRANCH, String.class);
        String sha1currentCommit = (String)
                readObject(BRANCHES, TreeMap.class).get(activeBranchName);
        Commit currentCommit = readObject(join(COMMITS, sha1currentCommit), Commit.class);

        /** if (the added file has the same contents as committed file, don't add to staging) */
        if (currentCommit.getAllfiles() != null) { //commit isn't empty
            /** first checks if the commit has the file,
             * if it does, checks if it has the same contents via SHA-1 */
            String contents = currentCommit.getAllfiles().get(filename);
            if (contents != null
                    && sha1(readContentsAsString(blobToAdd)).equals(contents)) {
                stagingareaadd.remove(filename);
                stagingarearm.remove(filename);
            } else {
                stagingareaadd.put(filename, sha1(readContentsAsString(blobToAdd)));
            }
        }
        writeObject(STAGING_AREA_ADD, stagingareaadd);
        writeObject(STAGING_AREA_RM, stagingarearm);
    }

    public static void addToRemovalArea(String filename) {
        File blobToRemove = join(CWD, filename);
        TreeMap<String, String> stagingareaadd = readObject(STAGING_AREA_ADD, TreeMap.class);
        TreeMap<String, String> stagingarearm = readObject(STAGING_AREA_RM, TreeMap.class);
        /** get the currentCommit that the headbranch is pointing to */
        String activeBranchName = readObject(HEADBRANCH, String.class);
        String sha1currentCommit = (String)
                readObject(BRANCHES, TreeMap.class).get(activeBranchName);
        Commit currentCommit = readObject(join(COMMITS, sha1currentCommit), Commit.class);

        if (currentCommit.getAllfiles() != null) {  //commit isn't empty
            /** checks if the file isn't in the commit + not staged, and fails */
            if (!currentCommit.getAllfiles().containsKey(filename)
                    && !stagingareaadd.containsKey(filename)) {
                System.out.println("No reason to remove the file.");
            }
            /** unstage if staged, stage for removal if tracked in the current commit */
            if (stagingareaadd.containsKey(filename)) {
                stagingareaadd.remove(filename);
            }
            /** if the file is tracked in the current commit,
             * stage it for removal and remove from CWD if user as not done so */
            if (currentCommit.getAllfiles().containsKey(filename)) {
                stagingarearm.put(filename, "YEET");
                /** remove file from working directory if the user has not already done so */
                blobToRemove.delete();
            }
        }
        writeObject(STAGING_AREA_ADD, stagingareaadd);
        writeObject(STAGING_AREA_RM, stagingarearm);
    }

    public static void commitThis(String msg) {
        TreeMap<String, String> stagingareaadd = readObject(STAGING_AREA_ADD, TreeMap.class);
        TreeMap<String, String> stagingarearm = readObject(STAGING_AREA_RM, TreeMap.class);
        if (stagingareaadd.size() == 0 && stagingarearm.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }
        /** creates the new commit and writes it to an File inside .gitlet directory */
        Commit newCommit = new Commit(msg);
        File newCommitFile = join(COMMITS, sha1(serialize(newCommit)));
        writeObject(newCommitFile, newCommit);

        /** sets whatever branch headbranch is pointing to to point to the new commit */
        String currentActiveBranchName = readObject(Repository.HEADBRANCH, String.class);
        TreeMap<String, String> branchesTM = readObject(Repository.BRANCHES, TreeMap.class);
        branchesTM.put(currentActiveBranchName, sha1(serialize(newCommit)));
        writeObject(Repository.BRANCHES, branchesTM);
        /** the head is still the name of the branch which points to this new commit */
    }

    public static void getLog() {
        String activeBranchName = readObject(HEADBRANCH, String.class);
        String sha1currentCommit =
                (String) readObject(BRANCHES, TreeMap.class).get(activeBranchName);
        Commit pointer = readObject(join(COMMITS, sha1currentCommit), Commit.class);
        while (true) {
            System.out.println("===");
            System.out.println("commit " + sha1(serialize(pointer)));
            if (pointer.getParent2() != null) {
                System.out.println("Merge: " + pointer.getParent1().substring(0, 7)
                        + " " + pointer.getParent2().substring(0, 7));
            }
            SimpleDateFormat ft = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            System.out.println("Date: " + ft.format(pointer.getTimeStamp()));
            System.out.println(pointer.getMessage());
            System.out.println();
            if (pointer.getMessage().equals("initial commit")) {
                break;
            }
            pointer = readObject(join(COMMITS, pointer.getParent1()), Commit.class);
        }
    }

    public static void getGlobalLog() {
        for (String commitName : plainFilenamesIn(COMMITS)) {
            Commit current = readObject(join(COMMITS, commitName), Commit.class);
            System.out.println("===");
            System.out.println("commit " + sha1(serialize(current)));
            if (current.getParent2() != null) {
                System.out.println("Merge: " + current.getParent1().substring(0, 7)
                        + " " + current.getParent2().substring(0, 7));
            }
            SimpleDateFormat ft = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
            System.out.println("Date: " + ft.format(current.getTimeStamp()));
            System.out.println(current.getMessage());
            System.out.println();
        }
    }

    public static void findCommit(String msg) {
        int numCommitsFound = 0;
        for (String commitName : plainFilenamesIn(COMMITS)) {
            Commit current = readObject(join(COMMITS, commitName), Commit.class);
            if (current.getMessage().equals(msg)) {
                System.out.println(sha1(serialize(current)));
                numCommitsFound++;
            }
        }
        if (numCommitsFound == 0) {
            System.out.println("Found no commit with that message");
        }
    }

    public static void getStatus() {
        System.out.println("=== Branches ===");
        String activeBranch = readObject(HEADBRANCH, String.class);
        System.out.println("*" + activeBranch);
        TreeMap<String, String> branchesTM =  readObject(BRANCHES, TreeMap.class);
        for (String branchName : branchesTM.keySet()) {
            if (!branchName.equals(activeBranch)) {
                System.out.println(branchName);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        TreeMap<String, String> stagingareaaddTm = readObject(STAGING_AREA_ADD, TreeMap.class);
        for (String fileName : stagingareaaddTm.keySet()) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        TreeMap<String, String> stagingarearmTM = readObject(STAGING_AREA_RM, TreeMap.class);
        for (String fileName : stagingarearmTM.keySet()) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }


    public static void checkout1(String filename) {
        File cwdFile = join(CWD, filename);
        String activeBranchName = readObject(HEADBRANCH, String.class);
        String sha1currentCommit =
                (String) readObject(BRANCHES, TreeMap.class).get(activeBranchName);
        Commit currentCommit = readObject(join(COMMITS, sha1currentCommit), Commit.class);
        if (currentCommit.getAllfiles().get(filename) == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String blobName = currentCommit.getAllfiles().get(filename);
        writeContents(cwdFile, readContentsAsString(join(BLOBS, blobName)));
    }

    public static void checkout2(String commitID, String filename) {
        commitID = uidHelper(commitID);
        File cwdFile = join(CWD, filename);
        boolean derp = !(plainFilenamesIn(COMMITS).contains(commitID));
        if (derp) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit wantedCommit = readObject(join(COMMITS, commitID), Commit.class);
        if (wantedCommit.getAllfiles().get(filename) == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String blobName = wantedCommit.getAllfiles().get(filename);
        //File checkoutBlob = readObject(join(BLOBS, blobName), File.class);
        writeContents(cwdFile, readContentsAsString(join(BLOBS, blobName)));
    }

    public static void checkout3(String branchName) {
        String sha1wantedCommit = (String) readObject(BRANCHES, TreeMap.class).get(branchName);
        if (sha1wantedCommit == null) {
            System.out.println("No such branch exists");
            return;
        }
        Commit checkedOutCommit = readObject(join(COMMITS, sha1wantedCommit), Commit.class);
        String headB = readObject(HEADBRANCH, String.class);
        TreeMap<String, String> branchesTM = readObject(BRANCHES, TreeMap.class);
        String currentCommitid = branchesTM.get(headB);
        Commit currentCommit = readObject(join(COMMITS, currentCommitid), Commit.class);
        TreeMap<String, String> stagingareaaddTm = readObject(STAGING_AREA_ADD, TreeMap.class);
        /** Checks if a file is untracked in the current branch
         * and would be overridden by the checkout */
        for (String cwdfileName : plainFilenamesIn(CWD)) {
            if (currentCommit.getAllfiles().get(cwdfileName) == null
                    && stagingareaaddTm.get(cwdfileName) == null
                    && checkedOutCommit.getAllfiles().containsKey(cwdfileName)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        if (branchName.equals(readObject(HEADBRANCH, String.class))) {
            System.out.println("No need to check out the current branch");
            return;
        }
        /** Takes all files in the checkedOutCommit and puts them in the CWD */
        for (String fileName : checkedOutCommit.getAllfiles().keySet()) {
            String blobName = checkedOutCommit.getAllfiles().get(fileName);
            File blobToAddToCWD = join(BLOBS, blobName);
            File fileFromCheckedOutCommit = join(CWD, fileName);
            writeContents(fileFromCheckedOutCommit, readContents(blobToAddToCWD));
        }
        /** Removes all files from the CWD tracked in the currentCommit
         * but not present in the checked out Commit */
        for (String cwdfileName : plainFilenamesIn(CWD)) {
            if (!checkedOutCommit.getAllfiles().containsKey(cwdfileName)
                    && currentCommit.getAllfiles().containsKey(cwdfileName)) {
                File toBeDeleted = join(CWD, cwdfileName);
                toBeDeleted.delete();
            }
        }
        /** given branch is now what the head points to */
        writeObject(HEADBRANCH, branchName);

        /** clears and the add and remove staging areas and saves them */
        TreeMap<String, String> stagingareaaddTreemap =
                readObject(Repository.STAGING_AREA_ADD, TreeMap.class);
        TreeMap<String, String> stagingarearmTreemap =
                readObject(Repository.STAGING_AREA_RM, TreeMap.class);
        stagingareaaddTreemap.clear();
        stagingarearmTreemap.clear();
        writeObject(Repository.STAGING_AREA_ADD, stagingareaaddTreemap);
        writeObject(Repository.STAGING_AREA_RM, stagingarearmTreemap);
    }

    public static void createBranch(String branchName) {
        TreeMap<String, String> branchesTM = readObject(BRANCHES, TreeMap.class);
        if (branchesTM.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        String headB = readObject(HEADBRANCH, String.class);
        String sha1currentCommit = branchesTM.get(headB);
        branchesTM.put(branchName, sha1currentCommit);
        writeObject(BRANCHES, branchesTM);
    }

    public static void deleteBranch(String branchName) {
        TreeMap<String, String> branchesTM = readObject(BRANCHES, TreeMap.class);
        String headB = readObject(HEADBRANCH, String.class);
        if (!branchesTM.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branchName.equals(headB)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branchesTM.remove(branchName);
        writeObject(BRANCHES, branchesTM);
    }

    public static void reset(String commitID) {
        commitID = uidHelper(commitID);
        boolean derp = !(plainFilenamesIn(COMMITS).contains(commitID));
        if (derp) {
            System.out.println("No commit with that id exists.");
            return;
        }
        /** create a temporary branch that points to commitID */
        TreeMap<String, String> branchesTM = readObject(BRANCHES, TreeMap.class);
        branchesTM.put("temporaryBranch", commitID);
        String currentBranch = readObject(HEADBRANCH, String.class);
        writeObject(BRANCHES, branchesTM);
        checkout3("temporaryBranch");

        /** delete 'temporaryBranch' and set the 'current' branch to this commit,
         * the head also marks the 'current' branch */
        branchesTM.put(currentBranch, commitID);
        writeObject(HEADBRANCH, currentBranch);
        branchesTM.remove("temporaryBranch");
        writeObject(BRANCHES, branchesTM);
    }

    public static void merge(String otherBranch) {
        String headB = readObject(HEADBRANCH, String.class);
        TreeMap<String, String> branchesTreemap = readObject(BRANCHES, TreeMap.class);
        Commit currentCommit = readObject(join(COMMITS, branchesTreemap.get(headB)), Commit.class);
        TreeMap<String, String> stagedareaaddTreemap = readObject(STAGING_AREA_ADD, TreeMap.class);
        TreeMap<String, String> stagedaarearmTreemap = readObject(STAGING_AREA_RM, TreeMap.class);
        for (String cwdfileName : plainFilenamesIn(CWD)) {
            if (currentCommit.getAllfiles().get(cwdfileName) == null
                    && stagedareaaddTreemap.get(cwdfileName) == null) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        if (stagedareaaddTreemap.size() + stagedaarearmTreemap.size() > 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (branchesTreemap.get(otherBranch) == null) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (otherBranch.equals(headB)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Queue<String> fringe = new LinkedList<>();
        Queue<String> storage = new LinkedList<>();

        fringe.add(sha1(serialize(currentCommit)));
        storage.add(sha1(serialize(currentCommit)));
        String splitPointid = null;
        /** This is the first BFS search on the current commit which head marks */
        while (fringe.size() != 0) {
            String commitVid = fringe.remove();
            Commit commitV = readObject(join(COMMITS, commitVid), Commit.class);
            if (commitV.getParent1() != null) {
                fringe.add(commitV.getParent1());
                storage.add(commitV.getParent1());
            }
            if (commitV.getParent2() != null) {
                fringe.add(commitV.getParent2());
                storage.add(commitV.getParent2());
            }
        }
        String commitidOther = branchesTreemap.get(otherBranch);
        Commit otherCommit = readObject(join(COMMITS, commitidOther), Commit.class);
        fringe.add(commitidOther);
        while (fringe.size() != 0) {
            String commitVid = fringe.remove();
            if (storage.contains(commitVid)) {
                splitPointid = commitVid;
                break;
            }
            Commit commitV = readObject(join(COMMITS, commitVid), Commit.class);
            if (commitV.getParent1() != null) {
                fringe.add(commitV.getParent1());
            }
            if (commitV.getParent2() != null) {
                fringe.add(commitV.getParent2());
            }
        }
        Commit splitPoint = readObject(join(COMMITS, splitPointid), Commit.class);
        if (otherCommit.getSha1().equals(splitPoint.getSha1())) {
            System.out.println("Given branch is an ancestor of the current branch");
            return;
        }
        if (currentCommit.getSha1().equals(splitPoint.getSha1())) {
            Repository.checkout3(otherBranch);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        boolean encounteredMerge = false;
        for (String fileName : splitPoint.getAllfiles().keySet()) {
            String sha1split = splitPoint.getAllfiles().get(fileName);
            String sha1other = otherCommit.getAllfiles().get(fileName);
            String sha1current = currentCommit.getAllfiles().get(fileName);
            /** CASE 1 */
            if (!totalCompare(sha1other, sha1split) && totalCompare(sha1current, sha1split)) {
                /** CASE 6 */
                if (totalCompare(sha1current, sha1split) && sha1other == null) {
                    Repository.addToRemovalArea(fileName);
                    continue;
                }
                stagedareaaddTreemap.put(fileName, otherCommit.getAllfiles().get(fileName));
                writeObject(STAGING_AREA_ADD, stagedareaaddTreemap);
            } else if (!totalCompare(sha1current, sha1split)
                    && totalCompare(sha1split, sha1other)) {   /** CASE 2: don't do anything */
                /** CASE 7 */
                if (totalCompare(sha1other, sha1split) && sha1current == null) {
                    continue;
                }
            } else if (!totalCompare(sha1current, sha1split)
                    && !totalCompare(sha1other, sha1split)) {  /** CASE 3 */
                if (totalCompare(sha1current, sha1other)) {
                    continue;
                } else {    /** CASE 8 or 3B*/
                    encounteredMerge = true;
                    String currContents = readContentsAsString(join(BLOBS, sha1current));
                    String otherContents = "";
                    if (sha1other != null) {
                        otherContents = readContentsAsString(join(BLOBS, sha1other));
                    }
                    writeContents(join(CWD, fileName), "<<<<<<< HEAD\n"
                            + currContents + "=======\n" + otherContents + ">>>>>>>\n");
                    Repository.addThisToStagingArea(fileName);
                }
            }

        }
        /** CASE 4: Files not in split and only present in current should stay as they are
         for (String fileName : currentCommit.allfiles.keySet()) {
         String sha1split = splitPoint.allfiles.get(fileName);
         String sha1other = otherCommit.allfiles.get(fileName);
         if (sha1split == null && sha1other == null) {

         }
         }*/
        /** CASE 5: Files not in split and only present in given should be checked out and staged */
        for (String fileName : otherCommit.getAllfiles().keySet()) {
            String sha1split = splitPoint.getAllfiles().get(fileName);
            String sha1current = currentCommit.getAllfiles().get(fileName);
            if (sha1split == null && sha1current == null) {
                Repository.checkout2(commitidOther, fileName);
                Repository.addThisToStagingArea(fileName);
            }
        }
        if (encounteredMerge) {
            System.out.println("Encountered a merge conflict.");
        }
        Repository.commitThis("Merged " + otherBranch + " into " + headB + ".");
        String newHead = readObject(HEADBRANCH, String.class);
        TreeMap<String, String> newBranches = readObject(BRANCHES, TreeMap.class);
        String resultId = newBranches.get(newHead);
        Commit result = readObject(join(COMMITS, resultId), Commit.class);
        result.setParent2(newBranches.get(otherBranch));

        /** Takes all files in the checkedOutCommit and puts them in the CWD */
        for (String fileName : result.getAllfiles().keySet()) {
            String blobName = result.getAllfiles().get(fileName);
            File blobToAddToCWD = join(BLOBS, blobName);
            File fileFromCheckedOutCommit = join(CWD, fileName);
            writeContents(fileFromCheckedOutCommit, readContents(blobToAddToCWD));
        }
        /** Removes all files from the CWD tracked in the currentCommit
         * but not present in the checked out Commit */
        for (String cwdfileName : plainFilenamesIn(CWD)) {
            if (!result.getAllfiles().containsKey(cwdfileName)
                    && currentCommit.getAllfiles().containsKey(cwdfileName)) {
                File toBeDeleted = join(CWD, cwdfileName);
                toBeDeleted.delete();
            }
        }
    }

    private static String uidHelper(String shortIdcommit) {
        String answer = "";
        for (String icommitId : plainFilenamesIn(COMMITS)) {
            if (icommitId.startsWith(shortIdcommit)) {
                answer = icommitId;
            }
        }
        if (answer.equals("")) {
            return shortIdcommit;
        }
        return answer;
    }

    private static String splitAncestor(Commit c1, Commit c2) {
        return "YEET";
    }

    private static boolean totalCompare(String s1, String s2) {
        /** cases where 1 of them is null */
        if (s1 == null && s2 != null) {
            return false;
        } else if (s1 != null && s2 == null) {
            return false;
        } else if (s1 != null && s2 != null) {   /** cases where none of them is null */
            return s1.equals(s2);
        }
        /** both are null */
        return true;
    }

}
