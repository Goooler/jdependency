/*
 * Copyright 2010-2024 The jdependency developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vafer.jdependency.asm;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * internal - do not use
 */
public final class DependenciesClassAdapter {

    private static final Pattern DESCRIPTOR_PATTERN = Pattern.compile("L([a-zA-Z0-9_/\\$]+);");

    private final Set<String> classes = new HashSet<>();

    public void accept(byte[] classBytes) {
        ClassModel cm = ClassFile.of().parse(classBytes);
        for (PoolEntry pe : cm.constantPool()) {
            if (pe instanceof ClassEntry ce) {
                String name = ce.asInternalName();
                if (name.startsWith("[")) {
                    Matcher m = DESCRIPTOR_PATTERN.matcher(name);
                    while (m.find()) {
                        classes.add(m.group(1).replace('/', '.'));
                    }
                } else {
                    classes.add(name.replace('/', '.'));
                }
            } else if (pe instanceof Utf8Entry ue) {
                String str = ue.stringValue();
                if (str.indexOf('L') != -1 && str.indexOf(';') != -1) {
                    Matcher m = DESCRIPTOR_PATTERN.matcher(str);
                    while (m.find()) {
                        classes.add(m.group(1).replace('/', '.'));
                    }
                }
            }
        }
    }

    public Set<String> getDependencies() {
        return classes;
    }
}
