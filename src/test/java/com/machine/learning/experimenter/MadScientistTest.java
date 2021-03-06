package com.machine.learning.experimenter;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


public class MadScientistTest {
    @Test
    @Ignore
    public void madScientistNoOp() {
        MadScientist madScientist = new MadScientist(Collections.emptyList(), Collections.emptyList());
        assertThat(madScientist.getResults(), is(equalTo("\\begin{table}\\n\\begin{tabular}{c|c|c|c|c|c|}\\n\\\\\\n\\hline\\n\\end{tabular}\\n\\end{table}")));
    }
}
