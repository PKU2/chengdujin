package yuan.jin.interviewQuestions;

import java.util.Vector;

/**
 * http://www-igm.univ-mlv.fr/~lecroq/string/node14.html
 * 
 * @author Yuan
 * 
 */
public class BoyerMooreAlgorithm {

	private static final int ALPHABET_SIZE = 0x10000; // UNICODE alphabet size
														// (64K chars)

	private int[] mBadCharSkipA; // one of the skip arrays (mismatched text char
									// not in pattern)
	private int[] mGoodSuffixSkipA; // the other skip array (suffix, mismatched
									// text char found in pattern)

	private char[] mPatternA; // chars of the pattern (what we're searching for)
	private int mPatternLength; // length of pattern

	private char[] mTextA; // chars of the text (the text we're searching in)
	private int mTextLength; // length of text

	// This stuff is used to split up the main loop so it can be single-stepped
	// and displayed.
	// The methods step(), skip(), and reset() are a chopped up version of the
	// main
	// search loop intended for single stepping.
	// The match() method is the same logic in more compact form (doesn't export
	// any of it's internal
	// state, and can't be single-stepped, but somewhat easier to grok).
	private int mMaxSkip = 0; // just stats
	private boolean mStopped = true; // set to true when we single-step off the
										// end of the text
	private int mCompareTextIndex; // index in text of next compare (exported
									// state)
	private int mComparePatternIndex; // index in pattern of next compare
										// (exported state)
	private char[] mStepA; // the results in symbolic form for display
	private int mPatternOffset; // index in text of beginning of pattern for
								// this compare
	private int mComparisonCount; // how many character comparisons have
									// happened so far

	private Vector mMatchV; // the set of all matches found in the text

	public BoyerMooreAlgorithm(String inPattern) {
		setPattern(inPattern);
	}

	/**
	 * inText is the text that will be searched in
	 */
	public void setText(String inText) {
		mTextA = inText.toCharArray();
		mTextLength = mTextA.length;

		mStepA = new char[mTextLength + 1]; // one more for stop '¥' char
		reset();
	}

	/**
	 * inPattern is the pattern that will be searched for
	 */
	public void setPattern(String inPattern) {
		mPatternA = inPattern.toCharArray();
		mPatternLength = mPatternA.length;
		mBadCharSkipA = new int[ALPHABET_SIZE]; // set to zero for free
		mGoodSuffixSkipA = new int[mPatternLength + 1]; // set to zero for free

		// recompute these arrays whenever the pattern changes
		computeGoodSuffixSkipArray();
		computeBadCharSkipArray();
	}

	/**
	 * Compute the good prefix skip array. The resulting mGoodSuffixSkipA is one
	 * of the important Boyer-Moore data structures. Print it out if you want to
	 * check program operation.
	 */
	private void computeGoodSuffixSkipArray() {
		int i;
		int j;
		int p;
		int[] f = new int[mPatternLength + 1];

		j = mPatternLength + 1;
		f[mPatternLength] = j;

		for (i = mPatternLength; i > 0; i--) {
			while (j <= mPatternLength && mPatternA[i - 1] != mPatternA[j - 1]) {
				if (mGoodSuffixSkipA[j] == 0) {
					mGoodSuffixSkipA[j] = j - i;
				}
				j = f[j];
			}
			f[i - 1] = --j;
		}

		p = f[0];
		for (j = 0; j <= mPatternLength; ++j) {
			if (mGoodSuffixSkipA[j] == 0) {
				mGoodSuffixSkipA[j] = p;
			}
			if (j == p) {
				p = f[p];
			}
		}
	}

	/**
	 * Compute the bad character skip array. The resulting mBadCharSkipA is one
	 * of the important Boyer-Moore data structures. Print it out if you want to
	 * check program operation.
	 */
	private void computeBadCharSkipArray() {
		for (int a = 0; a < ALPHABET_SIZE; a++) {
			mBadCharSkipA[a] = mPatternLength;
		}

		for (int j = 0; j < mPatternLength - 1; j++) {
			mBadCharSkipA[mPatternA[j]] = mPatternLength - j - 1;
		}
	}

	/**
	 * For reference, this is comparison loop in its pure form, this is the same
	 * logic that is split out in step(), skip(), and reset().
	 * 
	 * Returns a Vector of all the matches.
	 */
	public Vector match() {
		Vector v = new Vector();

		int i;
		int j;

		i = 0;
		while (i <= mTextLength - mPatternLength) {
			for (j = mPatternLength - 1; j >= 0
					&& mPatternA[j] == mTextA[i + j]; --j) {
				// note: empty loop while chars match
			}

			if (j < 0) { // off the left edge of the pattern, whole pattern
							// matched
							// System.out.println( "Pattern match at " + i );
				v.addElement(new Integer(i)); // add find to vector

				i += mGoodSuffixSkipA[0]; // always skip by suffix[0] after
											// match
											// makes sense, since we know the
											// last
											// comparison resulted in a match
											// and was
											// therefore found in the text
											// (therefore we
											// don't want to skip as a bad
											// character);
											// otherwise it's exactly like a
											// good prefix
											// mismatch at the 0'th position of
											// the pattern.
			} else { // character mismatch, skip
				i += Math.max( // take the biggest of the two possible skips
						mGoodSuffixSkipA[j + 1], // first one's suffix, second
													// one's bad char
						mBadCharSkipA[mTextA[i + j]] - mPatternLength + j + 1);
			}
		}

		return v; // return all matches
	}

	/**
	 * Execute one round of the same logic in the match() loop and stop. This
	 * allows the controller to see the state after each single step.
	 */
	public void step() {
		if (mStopped) { // done, or not initialized
			return;
		}

		mComparisonCount++; // count this round

		if (mPatternA[mComparePatternIndex] == mTextA[mCompareTextIndex]) { // a
																			// character
																			// matches
			if (mComparePatternIndex == 0) { // pattern matches
				mStepA[mCompareTextIndex] = '\u00a7'; // '¤' record found
														// pattern
				mMatchV.addElement(new Integer(mCompareTextIndex)); // add find
																	// to vector
				skip(mGoodSuffixSkipA[0]); // skip to next compare on match
			} else { // more pattern to compare
				mStepA[mCompareTextIndex] = '='; // '=' record successful char
													// compare
				mComparePatternIndex--;
				mCompareTextIndex--;
				mStepA[mCompareTextIndex] = '|';
			}
		} else {
			mStepA[mCompareTextIndex] = '\u2260'; // '­' record mismatch
			skip(Math.max(mGoodSuffixSkipA[mComparePatternIndex + 1],
					mBadCharSkipA[mTextA[mCompareTextIndex]] - mPatternLength
							+ mComparePatternIndex + 1));
		}
	}

	/**
	 * In single step mode, this moves the pattern forward by the appropriate
	 * amount when there is either a character mismatch, or the whole pattern
	 * matched.
	 * 
	 * The amount is determined by the caller and passed in as the inSkip
	 * argument.
	 */
	private void skip(int inSkip) {
		mComparePatternIndex = mPatternLength - 1;
		mPatternOffset += inSkip;
		mCompareTextIndex = mPatternOffset + mComparePatternIndex;

		if (mCompareTextIndex < mTextLength) {
			mStepA[mCompareTextIndex] = '|'; // '|' cursor
		} else {
			mStepA[mTextLength] = '\u00b7'; // '¥' end of search
			mStopped = true;
		}
	}

	/**
	 * Reset everything back to the start of the search so we can begin single
	 * stepping.
	 */
	public void reset() {
		mComparePatternIndex = mPatternLength - 1;
		mCompareTextIndex = mComparePatternIndex;
		mPatternOffset = 0;
		mComparisonCount = 0;
		for (int i = 0; i <= mTextLength; i++) { // forget comparison step
													// results
			mStepA[i] = ' ';
		}
		if (mCompareTextIndex < mTextLength) { // don't bother if text shorter
												// than pattern
			mStepA[mCompareTextIndex] = '|'; // cursor (next compare here)
		}
		mMatchV = new Vector(); // forget matches
		mStopped = false;
	}

	/**
	 * Just here so the controller can see our current state
	 */
	public Vector getSkipVector() {
		Vector outVector = new Vector(mPatternLength);

		for (int i = 0; i < mPatternLength; i++) {
			outVector.addElement(new Integer(mGoodSuffixSkipA[i]));
		}
		return outVector;
	}

	/**
	 * Just here so the controller can see our current state
	 */
	public int getMaxSkip() {
		return mMaxSkip;
	}

	/**
	 * Just here so the controller can see our current state
	 */
	private void calcMaxSkip() {
		for (int i = 0; i < mPatternLength; i++) {
			int theSkip = mGoodSuffixSkipA[mPatternA[i]];
			if (theSkip < mPatternLength && theSkip > mMaxSkip) {
				mMaxSkip = theSkip;
			}
		}
	}

	/**
	 * Just here so the controller can see our current state
	 */
	public boolean isStopped() {
		return mStopped;
	}

	/**
	 * Just here so the controller can see our current state
	 */
	public int getCompareTextIndex() {
		return mCompareTextIndex;
	}

	/**
	 * Just here so the controller can see our current state
	 */
	public int getComparePatternIndex() {
		return mComparePatternIndex;
	}

	/**
	 * Just here so the controller can see our current state
	 */
	public int getPatternOffset() {
		return mPatternOffset;
	}

	/**
	 * Just here so the controller can see our current state
	 */
	public int getComparisonCount() {
		return mComparisonCount;
	}

	/**
	 * Just here so the controller can see our current state
	 */
	public String getStepString() {
		return new String(mStepA);
	}

	public static void main(String[] args) {
		BoyerMooreAlgorithm search = new BoyerMooreAlgorithm("abcd");
		search.setText("xyzabcdefg");

		Vector v = search.getSkipVector();
		if (search.getMaxSkip() < 10) {
			StringBuffer b = new StringBuffer(v.size());
			for (int i = 0; i < v.size(); i++)
				b.append(v.elementAt(i));
			System.out.println(b.toString());
		} else
			System.out.println(v.toString());

	}

}
