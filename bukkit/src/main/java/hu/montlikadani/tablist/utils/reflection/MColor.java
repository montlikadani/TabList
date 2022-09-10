package hu.montlikadani.tablist.utils.reflection;

enum MColor {

	BLACK('0'),
	DARK_BLUE('1'),
	DARK_GREEN('2'),
	DARK_AQUA('3'),
	DARK_RED('4'),
	DARK_PURPLE('5'),
	GOLD('6'),
	GRAY('7'),
	DARK_GRAY('8'),
	BLUE('9'),
	GREEN('a'),
	AQUA('b'),
	RED('c'),
	LIGHT_PURPLE('d'),
	YELLOW('e'),
	WHITE('f'),

	OBFUSCATED('k', "obfuscated"),
	BOLD('l', "bold"),
	STRIKETHROUGH('m', "strikethrough"),
	UNDERLINE('n', "underlined"),
	ITALIC('o', "italic"),

	RESET('r');

	public static final MColor[] VALUES = MColor.values();

	public final char code;
	public final boolean formatter;
	public final String propertyName;

	private MColor(char code) {
		this.code = code;
		formatter = false;
		propertyName = name().toLowerCase(java.util.Locale.ENGLISH);
	}

	private MColor(char code, String propertyName) {
		this.code = code;
		formatter = true;
		this.propertyName = propertyName;
	}

	public static MColor byCode(char code) {
		for (MColor mColor : VALUES) {
			if (mColor.code == code) {
				return mColor;
			}
		}

		return null;
	}
}
