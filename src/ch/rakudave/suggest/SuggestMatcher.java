package ch.rakudave.suggest;

public interface SuggestMatcher {
	public boolean matches(String dataWord, String searchWord);
}