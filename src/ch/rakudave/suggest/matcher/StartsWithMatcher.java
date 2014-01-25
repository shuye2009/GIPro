package ch.rakudave.suggest.matcher;

import ch.rakudave.suggest.SuggestMatcher;

public class StartsWithMatcher implements SuggestMatcher {
	@Override
	public boolean matches(String dataWord, String searchWord) {
		return dataWord.startsWith(searchWord);
	}
}