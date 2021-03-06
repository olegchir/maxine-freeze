/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.vm.ext.graal.phases;

import static com.oracle.graal.phases.GraalOptions.*;

import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.common.InliningUtil.*;
import com.oracle.max.vm.ext.graal.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.hosted.*;

/**
 * This is a Maxine customization for tuning the inlining policy.
 * Since Maxine currently does not support runtime generated probabilities,
 * we use the {@code min(relevance, probability)} in {@link Policy#computeMaximumSize}.
 *
 * We also dial down the {@link GraalOptions#MaximumInliningSize} value here, rather than
 * change the actual option.
 */
public class MaxInliningPhase extends InliningPhase {

    @RESET
    private static int maxInliningSize;

    private static int getMaxInliningSize() {
        if (maxInliningSize == 0) {
            int optionValue = MaximumInliningSize.getValue();
            // If it was set on the command line, assume user knows what they are doing
            if (MaxGraalOptions.isPresent("MaximumInliningSize") == null) {
                if (optionValue > 100) {
                    optionValue = 100;
                }
            }
            maxInliningSize = optionValue;
        }
        return maxInliningSize;
    }

    static {
        JavaPrototype.registerInitializationCompleteCallback(new Callback());
    }


    @HOSTED_ONLY
    private static class Callback implements JavaPrototype.InitializationCompleteCallback {

        public void initializationComplete() {
            // Here you could capture the Actors for specific methods/classes for comparison in isWorthInlining.
        }

    }

    private static class ProbTL extends ThreadLocal<Double> {

    }

    private static final ProbTL probTL = new ProbTL();

    static class Policy extends GreedyInliningPolicy {

        public Policy() {
            super(null);
        }

        /**
         * Maxine-specific computation. N.B. This is highly dependent on the implementation in the supertype.
         * In particular, we assume {@code configuredMaximum == (int) (MaximumInliningSize.getValue() * inliningBonus)}.
         */
        @Override
        protected double computeMaximumSize(double relevance, int configuredMaximum) {
            // recover inliningBonus (some loss of precision due to the original conversion to int is inevitable)
            double inliningBonus = (double) configuredMaximum / (double) MaximumInliningSize.getValue();
            return super.computeMaximumSize(Math.min(relevance, probTL.get()), (int) (getMaxInliningSize() * inliningBonus));
        }

        @Override
        public boolean isWorthInlining(Replacements replacements, InlineInfo info, int inliningDepth, double probability, double relevance, boolean fullyProcessed) {
            probTL.set(probability);
            return super.isWorthInlining(replacements, info, inliningDepth, probability, relevance, fullyProcessed);
        }
    }

    public MaxInliningPhase() {
        super(new Policy());
    }

}
