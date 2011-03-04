package com.mebigfatguy.blocklist;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class BlockListTest {

	@Test
	public void testAdd20() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 20; i++) {
			bl.add("Hello" + i);
		}
		Assert.assertEquals(20, bl.size());
	}

	@Test
	public void testAdd20ToFront() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 20; i++) {
			bl.add(0, "Hello" + i);
		}
		Assert.assertEquals(20, bl.size());
	}

	@Test
	public void testGet20() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 20; i++) {
			bl.add(i, "Hello" + i);
		}
		for (int i = 0; i < 20; i++) {
			Assert.assertEquals("Hello" + i, bl.get(i));
		}
	}
	
	@Test
	public void testRemove20() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 20; i++) {
			bl.add(i, "Hello" + i);
		}
		for (int i = 0; i < 20; i++) {
			bl.remove(0);
		}
		for (int i = 0; i < 20; i++) {
			bl.add(i, "Hello" + i);
		}
		for (int i = 19; i >= 0; i--) {
			bl.remove(0);
		}		
		Assert.assertEquals(0, bl.size());
	}
	
	@Test
	public void testRemoveAll20() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 20; i++) {
			bl.add(i, "Hello" + i);
		}
		bl.removeAll(Arrays.asList(new String[] { "Hello2", "Hello17"}));
		Assert.assertEquals(18, bl.size());
	}
	
	@Test
	public void testRetainAll20() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 20; i++) {
			bl.add(i, "Hello" + i);
		}
		bl.retainAll(Arrays.asList(new String[] { "Hello2", "Hello17"}));
		Assert.assertEquals(2, bl.size());
	}
	
	@Test
	public void testToArray() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 20; i++) {
			bl.add(i, "Hello" + i);
		}
		
		String[] result = bl.toArray(new String[0]);
		Assert.assertEquals(20, result.length);
		
		result = bl.toArray(new String[20]);
		Assert.assertEquals(20, result.length);		
	}
	
	@Test
	public void testIterator() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 20; i++) {
			bl.add(i, "Hello" + i);
		}
		
		int pos = 0;
		Iterator<String> it = bl.iterator();
		while (it.hasNext()) {
			Assert.assertEquals("Hello" + pos++, it.next());
		}
	}
}
