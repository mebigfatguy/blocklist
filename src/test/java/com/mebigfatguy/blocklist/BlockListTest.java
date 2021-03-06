package com.mebigfatguy.blocklist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class BlockListTest {

	@Test
	public void testAdd70() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 70; i++) {
			bl.add("Hello" + i);
		}
		Assert.assertEquals(70, bl.size());
	}

	@Test
	public void testAdd70ToFront() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 70; i++) {
			bl.add(0, "Hello" + i);
		}
		Assert.assertEquals(70, bl.size());
	}

	@Test
	public void testGet70() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 70; i++) {
			bl.add(i, "Hello" + i);
		}
		for (int i = 0; i < 70; i++) {
			Assert.assertEquals("Hello" + i, bl.get(i));
		}
	}

	@Test
	public void testRemove70() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 70; i++) {
			bl.add(i, "Hello" + i);
		}
		for (int i = 0; i < 70; i++) {
			bl.remove(0);
		}
		for (int i = 0; i < 70; i++) {
			bl.add(i, "Hello" + i);
		}
		for (int i = 69; i >= 0; i--) {
			bl.remove(0);
		}
		Assert.assertEquals(0, bl.size());
	}

	@Test
	public void testRemoveAll70() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 70; i++) {
			bl.add(i, "Hello" + i);
		}
		bl.removeAll(Arrays.asList(new String[] { "Hello2", "Hello17"}));
		Assert.assertEquals(68, bl.size());
	}

	@Test
	public void testRetainAll70() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 70; i++) {
			bl.add(i, "Hello" + i);
		}
		bl.retainAll(Arrays.asList(new String[] { "Hello2", "Hello17"}));
		Assert.assertEquals(2, bl.size());
	}

	@Test
	public void testToArray() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 70; i++) {
			bl.add(i, "Hello" + i);
		}

		String[] result = bl.toArray(new String[0]);
		Assert.assertEquals(70, result.length);

		result = bl.toArray(new String[70]);
		Assert.assertEquals(70, result.length);
	}

	@Test
	public void testIterator() {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 70; i++) {
			bl.add(i, "Hello" + i);
		}

		int pos = 0;
		Iterator<String> it = bl.iterator();
		while (it.hasNext()) {
			Assert.assertEquals("Hello" + pos++, it.next());
		}
	}
	
	@Test
	public void testContains() {
	    BlockList<String> bl = new BlockList<String>();
        for (int i = 0; i < 70; i++) {
            bl.add(i, "Hello" + i);
        }
        
        for (String s : bl) {
            Assert.assertTrue(bl.contains(s));
        }
        
        Assert.assertFalse(bl.contains("foobar"));
	}
	
	@Test 
	public void testIndexOfs() {
       BlockList<String> bl = new BlockList<String>();
       for (int i = 0; i < 70; i++) {
           bl.add(i, "Hello" + i);
       }
       
       Assert.assertEquals(2, bl.indexOf("Hello" + 2));
       Assert.assertEquals(66, bl.lastIndexOf("Hello" + 66));     
       Assert.assertEquals(-1, bl.lastIndexOf("Hello" + 100));     
	}
	
	@Test
	public void testInsertAtBlockEdge() {
	    BlockList<String> bl = new BlockList<String>(5);
	    
	    for (int i = 0; i < 20; i++) {
	        bl.add("Hello" + i);
	    }
	    
	    bl.add(14, "Insert" + 14);
        bl.add(5, "Insert" + 5);
        bl.add(1, "Insert" + 1);

	    
	    Assert.assertEquals(23, bl.size());
        Assert.assertEquals("Insert" + 1, bl.get(1));
        Assert.assertEquals("Insert" + 5, bl.get(6));
        Assert.assertEquals("Insert" + 14, bl.get(16));
	}
	@Test
	public void testRetainAll() {
       BlockList<String> bl1 = new BlockList<String>();
       for (int i = 0; i < 70; i++) {
           bl1.add(i, "Hello" + i);
       }
       
       BlockList<String> bl2 = new BlockList<String>();
       for (int i = 50; i < 100; i++) {
           bl2.add(i, "Hello" + i);
       }
       
       bl1.retainAll(bl2);
       
       Assert.assertEquals(20, bl1.size());
       Assert.assertFalse(bl1.contains("Hello" + 49));
       Assert.assertTrue(bl1.contains("Hello" + 50));
       Assert.assertTrue(bl1.contains("Hello" + 69));	       
       Assert.assertFalse(bl1.contains("Hello" + 70));
	}

	@Test
	public void testSerialization() throws IOException, ClassNotFoundException {
		BlockList<String> bl = new BlockList<String>();
		for (int i = 0; i < 70; i++) {
			bl.add(i, "Hello" + i);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(bl);
		oos.flush();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);

		Object sbl = ois.readObject();

		Assert.assertEquals(bl, sbl);
	}
	
	@Test(expected=ConcurrentModificationException.class)
	public void testConcurrentModificationException() {
	    BlockList<String> bl = new BlockList<String>();
        for (int i = 0; i < 70; i++) {
            bl.add(i, "Hello" + i);
        }
        
        for (String s : bl) {
            if (s.equals("Hello" + 4)) {
                bl.remove("Hello" + 4);
            }
        }
	}
}
