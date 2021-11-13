package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Anthony Zhang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please eneter a command");
            return;
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                Repository.initialCommit();
                break;
            case "add":
                Repository.addThisToStagingArea(args[1]);
                break;
            case "commit":
                if (args.length == 1 || args[1].equals("")) {
                    System.out.println("Please enter a commit message");
                    break;
                }
                Repository.commitThis(args[1]);
                break;
            case "rm":
                Repository.addToRemovalArea(args[1]);
                break;
            case "log":
                Repository.getLog();
                break;
            case "global-log":
                Repository.getGlobalLog();
                break;
            case "find":
                Repository.findCommit(args[1]);
                break;
            case "status":
                if (!Repository.GITLET_DIR.isDirectory()) {
                    System.out.println("Not in an initialized Gitlet directory");
                    break;
                }
                Repository.getStatus();
                break;
            case "checkout":
                if (args[1].equals("--")) {
                    Repository.checkout1(args[2]);
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect Operands");
                        break;
                    }
                    Repository.checkout2(args[1], args[3]);
                } else {
                    Repository.checkout3(args[1]);
                }
                break;
            case "branch":
                Repository.createBranch(args[1]);
                break;
            case "rm-branch":
                Repository.deleteBranch(args[1]);
                break;
            case "reset":
                Repository.reset(args[1]);
                break;
            case "merge":
                Repository.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists");
                break;
        }
    }
}
