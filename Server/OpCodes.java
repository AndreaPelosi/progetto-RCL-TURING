import java.util.HashMap;

public enum OpCodes {

	/***** OPERAZIONI CHE IL SERVER DEVE GESTIRE *****/
	LOGIN_OP(0),
	LOGOUT_OP(1),
	CREATE_OP(2),
	INVITE_OP(3),
	EDIT_OP(4),
	LIST_OP(5),
	SHOWSEC_OP(6),
	SHOWDOC_OP(7),
	ENDEDIT_OP(8),
	
	
	/***** OPERAZIONI DI RISPOSTA DEL SERVER *****/
	OP_OK(10),
	OP_FAIL(11),
	OP_LOGINFIRST(12),
	OP_NOUSER(13),
	OP_WRONGPASSWORD(14),
	OP_DOCNAMETAKEN(15),
	OP_FAILEDCREATE(16),
	OP_NOSUCHDOC(17),
	OP_NOTPERMITTED(18),
	OP_EDITSTATE(19),
	OP_ILLEGALSECTION(20),
	OP_SECTIONTAKEN(21),
	OP_NOTEDITOR(22),
	OP_KICKEDEDITOR(23);
	
	
	private int value;
	private static HashMap<Object, Object> map = new HashMap<>();
	
	private OpCodes(int value) {
		this.value = value;
	}
	
    static {
        for (OpCodes opcode : OpCodes.values()) {
            map.put(opcode.value, opcode);
        }
    }
	

    public static OpCodes valueOf(int opcode) {
        return (OpCodes) map.get(opcode);
    }

    public int getValue() {
        return value;
    }

}
