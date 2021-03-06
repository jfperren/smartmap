package ch.epfl.smartmap.test.severcom;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import ch.epfl.smartmap.servercom.JsonSmartMapParser;
import ch.epfl.smartmap.servercom.SmartMapParseException;
import ch.epfl.smartmap.servercom.SmartMapParser;

public class ParseFriendMalformedJSONParsingTest extends TestCase {

	private static final String FRIEND_JSON = "{\n" + " \"id\" : \"13\", \n"
			+ " \"name\" : \"Georges\", \n" + " \"latitude\" : \"20.03\", \n"
			+ " \"longitude\" : \"26.85\" \n" + "}\n";

	private List<String> friendJsonFields;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		friendJsonFields = new ArrayList<String>();
		friendJsonFields.add("id");
		friendJsonFields.add("name");

	}

	@Test
	public void testParseFriendEmptyJson() {
		SmartMapParser parser = new JsonSmartMapParser();
		try {
			parser.parseFriend(new JSONObject().toString());
			fail("parsed empty Json");
		} catch (SmartMapParseException e) {
			// success
		}
	}

	@Test
	public void testParseFriendEmptyName() throws JSONException {
		JSONObject jsonObject = new JSONObject(FRIEND_JSON);
		jsonObject.put("name", "");
		SmartMapParser parser = new JsonSmartMapParser();

		try {
			parser.parseFriend(jsonObject.toString());
			fail("parsed empty name");
		} catch (SmartMapParseException e) {
			// success
		}
	}

	@Test
	public void testParseFriendMissingId() throws JSONException {
		JSONObject jsonObject = new JSONObject(FRIEND_JSON);
		jsonObject.remove("id");
		SmartMapParser parser = new JsonSmartMapParser();
		try {
			parser.parseFriend(jsonObject.toString());
			fail("missing id");
		} catch (SmartMapParseException e) {
			// success
		}

	}

	@Test
	public void testParseFriendMissingName() throws JSONException {
		JSONObject jsonObject = new JSONObject(FRIEND_JSON);
		jsonObject.remove("name");
		SmartMapParser parser = new JsonSmartMapParser();
		try {
			parser.parseFriend(jsonObject.toString());
			fail("missing name");
		} catch (SmartMapParseException e) {
			// success
		}
	}

	@Test
	public void testParseFriendTooLongName() throws JSONException {
		JSONObject jsonObject = new JSONObject(FRIEND_JSON);
		jsonObject
		.put("name",
				"egrhgpiergbpwifbowiegforwgtoiedfbéwgfboéwagrfowéargforwgfowaugfowiegfowaifgoawéietgéwagprigfsgfgjkaegoirgorigéraoigpwrgoéwigaowigfvbrofgroivrhtoghroufgvborthgoéaiegéorifgoga");
		SmartMapParser parser = new JsonSmartMapParser();

		try {
			parser.parseFriend(jsonObject.toString());
			fail("parsed too long name");
		} catch (SmartMapParseException e) {
			// success
		}
	}

	@Test
	public void testParseFriendWrongId() throws JSONException {
		JSONObject jsonObject = new JSONObject(FRIEND_JSON);
		jsonObject.put("id", -3);
		SmartMapParser parser = new JsonSmartMapParser();

		try {
			parser.parseFriend(jsonObject.toString());
			fail("parsed negative id");
		} catch (SmartMapParseException e) {
			// success
		}
	}

}