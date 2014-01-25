package ch.rakudave.suggest.matcher;

import ch.rakudave.suggest.SuggestMatcher;

public class ContainsMatcher implements SuggestMatcher {
	@Override
	public boolean matches(String dataWord, String searchWord) {
		return dataWord.contains(searchWord);
	}
}