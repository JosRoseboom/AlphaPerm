package alphabetpermutation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Starter {
	
	public static void main(String[] args){
		
		String wordListLocation		=	"/home/jos/Documents/english_words.txt";
		List<Character> chars		=	new ArrayList<Character>();
		List<String> words			=	null;
		
		for(int i=0;i<26;i++)
			chars.add(new Character((char)('A' + i)));
				
		try {
			words = Files.readAllLines(Paths.get(wordListLocation), Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(words != null)
			DistributedOptimizer.optimize(new ArrayList<Character>(chars), getVictimsPerCombination(chars, words), 4);
	}
	
	/**
	 * @param chars the allowed chars
	 * @param words the words to test against
	 * @return a set of indices of words that are invalid if the first key precedes the second key in a permutation.
	 */
	private static Map<Character, Map<Character, Set<Integer>>> getVictimsPerCombination(List<Character> chars,
			List<String> words) {
		
		Map<Character, Map<Character, Set<Integer>>> result	=	new HashMap<Character, Map<Character, Set<Integer>>>();
		
		// init map
		for(Character firstChar : chars){
			result.put(firstChar, new HashMap<Character, Set<Integer>>());
			for(Character secondChar : chars)
				if(!firstChar.equals(secondChar))
					result.get(firstChar).put(secondChar, new HashSet<Integer>());
		}
					
		for(int wordIndex = 0; wordIndex < words.size(); wordIndex++){
			String word	=	words.get(wordIndex);
			for(int firstCharIndex = 0; firstCharIndex < word.length(); firstCharIndex++)
				for(int secondCharIndex = firstCharIndex + 1; secondCharIndex < word.length(); secondCharIndex++){
					Character firstChar	=	word.charAt(firstCharIndex);
					Character secondChar=	word.charAt(secondCharIndex);
					if(!firstChar.equals(secondChar))
						result.get(secondChar).get(firstChar).add(wordIndex);
				}
		}

		return result;
	}
}