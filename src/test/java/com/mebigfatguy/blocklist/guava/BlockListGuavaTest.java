package com.mebigfatguy.blocklist.guava;

import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import com.google.common.collect.testing.ListTestSuiteBuilder;
import com.google.common.collect.testing.TestStringListGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.mebigfatguy.blocklist.BlockList;

import junit.framework.TestSuite;

@RunWith(AllTests.class)
public class BlockListGuavaTest {

    public static TestSuite suite() {
        return ListTestSuiteBuilder.using(new TestStringListGenerator() {

            @Override
            protected List<String> create(String[] entries) {
                List<String> list = new BlockList<>(entries.length);
                for (String entry : entries) {
                    list.add(entry);
                }
                return list;
            }

        }).named("Guava List Test").withFeatures(CollectionSize.ANY, CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION, ListFeature.GENERAL_PURPOSE)
                .createTestSuite();
    }
}
