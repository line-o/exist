// $ANTLR : "XPathParser2.g" -> "XPathParser2.java"$

	package org.exist.parser;

	import antlr.debug.misc.*;
	import java.io.StringReader;
	import java.io.BufferedReader;
	import java.io.InputStreamReader;
	import java.util.ArrayList;
	import java.util.List;
	import java.util.Iterator;
	import java.util.Stack;
	import org.exist.storage.BrokerPool;
	import org.exist.storage.DBBroker;
	import org.exist.storage.analysis.Tokenizer;
	import org.exist.EXistException;
	import org.exist.dom.DocumentSet;
	import org.exist.dom.DocumentImpl;
	import org.exist.dom.QName;
	import org.exist.security.PermissionDeniedException;
	import org.exist.security.User;
	import org.exist.xpath.*;
	import org.exist.xpath.value.*;
	import org.exist.xpath.functions.*;

public interface XPathParser2TokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int QNAME = 4;
	int PREDICATE = 5;
	int FLWOR = 6;
	int PARENTHESIZED = 7;
	int ABSOLUTE_SLASH = 8;
	int ABSOLUTE_DSLASH = 9;
	int WILDCARD = 10;
	int PREFIX_WILDCARD = 11;
	int FUNCTION = 12;
	int UNARY_MINUS = 13;
	int UNARY_PLUS = 14;
	int XPOINTER = 15;
	int XPOINTER_ID = 16;
	int VARIABLE_REF = 17;
	int VARIABLE_BINDING = 18;
	int ELEMENT = 19;
	int ATTRIBUTE = 20;
	int TEXT = 21;
	int VERSION_DECL = 22;
	int NAMESPACE_DECL = 23;
	int DEF_NAMESPACE_DECL = 24;
	int DEF_FUNCTION_NS_DECL = 25;
	int GLOBAL_VAR = 26;
	int FUNCTION_DECL = 27;
	int PROLOG = 28;
	int ATOMIC_TYPE = 29;
	int MODULE = 30;
	int ORDER_BY = 31;
	int POSITIONAL_VAR = 32;
	int LITERAL_xpointer = 33;
	int LPAREN = 34;
	int RPAREN = 35;
	int NCNAME = 36;
	int XQUERY = 37;
	int VERSION = 38;
	int SEMICOLON = 39;
	int LITERAL_declare = 40;
	int LITERAL_namespace = 41;
	int LITERAL_default = 42;
	int LITERAL_function = 43;
	int LITERAL_variable = 44;
	int STRING_LITERAL = 45;
	int EQ = 46;
	int LITERAL_element = 47;
	int DOLLAR = 48;
	int LCURLY = 49;
	int RCURLY = 50;
	int LITERAL_as = 51;
	int COMMA = 52;
	int LITERAL_empty = 53;
	int QUESTION = 54;
	int STAR = 55;
	int PLUS = 56;
	int LITERAL_item = 57;
	int LITERAL_for = 58;
	int LITERAL_let = 59;
	int LITERAL_if = 60;
	int LITERAL_where = 61;
	int LITERAL_return = 62;
	int LITERAL_in = 63;
	int LITERAL_at = 64;
	int COLON = 65;
	int LITERAL_order = 66;
	int LITERAL_by = 67;
	int LITERAL_ascending = 68;
	int LITERAL_descending = 69;
	int LITERAL_greatest = 70;
	int LITERAL_least = 71;
	int LITERAL_then = 72;
	int LITERAL_else = 73;
	int LITERAL_or = 74;
	int LITERAL_and = 75;
	int LITERAL_cast = 76;
	int LITERAL_eq = 77;
	int LITERAL_ne = 78;
	int LITERAL_lt = 79;
	int LITERAL_le = 80;
	int LITERAL_gt = 81;
	int LITERAL_ge = 82;
	int NEQ = 83;
	int GT = 84;
	int GTEQ = 85;
	int LT = 86;
	int LTEQ = 87;
	int ANDEQ = 88;
	int OREQ = 89;
	int LITERAL_to = 90;
	int MINUS = 91;
	int LITERAL_div = 92;
	int LITERAL_idiv = 93;
	int LITERAL_mod = 94;
	int LITERAL_union = 95;
	int UNION = 96;
	int LITERAL_intersect = 97;
	int LITERAL_except = 98;
	int SLASH = 99;
	int DSLASH = 100;
	int LITERAL_text = 101;
	int LITERAL_node = 102;
	int SELF = 103;
	int XML_COMMENT = 104;
	int LPPAREN = 105;
	int RPPAREN = 106;
	int AT = 107;
	int PARENT = 108;
	int LITERAL_child = 109;
	int LITERAL_self = 110;
	int LITERAL_attribute = 111;
	int LITERAL_descendant = 112;
	// "descendant-or-self" = 113
	// "following-sibling" = 114
	int LITERAL_parent = 115;
	int LITERAL_ancestor = 116;
	// "ancestor-or-self" = 117
	// "preceding-sibling" = 118
	int DOUBLE_LITERAL = 119;
	int DECIMAL_LITERAL = 120;
	int INTEGER_LITERAL = 121;
	int LITERAL_comment = 122;
	// "processing-instruction" = 123
	// "document-node" = 124
	int WS = 125;
	int END_TAG_START = 126;
	int QUOT = 127;
	int ATTRIBUTE_CONTENT = 128;
	int ELEMENT_CONTENT = 129;
	int XML_COMMENT_END = 130;
	int XML_PI = 131;
	int XML_PI_END = 132;
	int LITERAL_document = 133;
	int LITERAL_collection = 134;
	int XML_PI_START = 135;
	int LETTER = 136;
	int DIGITS = 137;
	int HEX_DIGITS = 138;
	int NMSTART = 139;
	int NMCHAR = 140;
	int EXPR_COMMENT = 141;
	int PREDEFINED_ENTITY_REF = 142;
	int CHAR_REF = 143;
	int NEXT_TOKEN = 144;
	int CHAR = 145;
	int BASECHAR = 146;
	int IDEOGRAPHIC = 147;
	int COMBINING_CHAR = 148;
	int DIGIT = 149;
	int EXTENDER = 150;
}
