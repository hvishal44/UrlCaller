import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;

public class Solution {


	public static void main(String[] args) {
		Calendar c = Calendar.getInstance();
//		c.set(Calendar.YEAR, 2015);
//		c.set(Calendar.MONTH,8);
//		c.set(Calendar.DATE,05);
//		Date date = new Date();
		c.set(2059, 11,25 );
		System.out.println(c.get(Calendar.DAY_OF_WEEK));
	}

}
