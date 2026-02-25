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

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * internal - do not use
 */
public final class DependenciesClassAdapter {

    private static final Pattern SIGNATURE_CLASS_PATTERN =
        Pattern.compile("L([a-zA-Z0-9_$]+(?:/[a-zA-Z0-9_$]+)*)");

    private final Set<String> classes = new HashSet<>();

    public void accept(byte[] classBytes) {
        ClassModel cm = ClassFile.of().parse(classBytes);

        for (PoolEntry pe : cm.constantPool()) {
            if (pe instanceof ClassEntry ce) {
                collectFromInternalName(ce.asInternalName());
            }
        }

        for (FieldModel fm : cm.fields()) {
            collectFromClassDesc(fm.fieldTypeSymbol());
            fm.findAttribute(Attributes.signature()).ifPresent(sa ->
                collectFromSignature(sa.signature().stringValue()));
            fm.findAttribute(Attributes.runtimeVisibleAnnotations()).ifPresent(a ->
                a.annotations().forEach(ann -> collectFromClassDesc(ann.classSymbol())));
            fm.findAttribute(Attributes.runtimeInvisibleAnnotations()).ifPresent(a ->
                a.annotations().forEach(ann -> collectFromClassDesc(ann.classSymbol())));
        }

        for (MethodModel mm : cm.methods()) {
            collectFromMethodTypeDesc(mm.methodTypeSymbol());
            mm.findAttribute(Attributes.exceptions()).ifPresent(ea ->
                ea.exceptions().forEach(ce -> collectFromInternalName(ce.asInternalName())));
            mm.findAttribute(Attributes.signature()).ifPresent(sa ->
                collectFromSignature(sa.signature().stringValue()));
            mm.findAttribute(Attributes.runtimeVisibleAnnotations()).ifPresent(a ->
                a.annotations().forEach(ann -> collectFromClassDesc(ann.classSymbol())));
            mm.findAttribute(Attributes.runtimeInvisibleAnnotations()).ifPresent(a ->
                a.annotations().forEach(ann -> collectFromClassDesc(ann.classSymbol())));
            mm.findAttribute(Attributes.runtimeVisibleParameterAnnotations()).ifPresent(a ->
                a.parameterAnnotations().forEach(list ->
                    list.forEach(ann -> collectFromClassDesc(ann.classSymbol()))));
            mm.findAttribute(Attributes.runtimeInvisibleParameterAnnotations()).ifPresent(a ->
                a.parameterAnnotations().forEach(list ->
                    list.forEach(ann -> collectFromClassDesc(ann.classSymbol()))));
        }

        cm.findAttribute(Attributes.signature()).ifPresent(sa ->
            collectFromSignature(sa.signature().stringValue()));
        cm.findAttribute(Attributes.runtimeVisibleAnnotations()).ifPresent(a ->
            a.annotations().forEach(ann -> collectFromClassDesc(ann.classSymbol())));
        cm.findAttribute(Attributes.runtimeInvisibleAnnotations()).ifPresent(a ->
            a.annotations().forEach(ann -> collectFromClassDesc(ann.classSymbol())));
    }

    private void collectFromInternalName(String internalName) {
        if (internalName.startsWith("[")) {
            collectFromDescriptor(internalName);
        } else {
            classes.add(internalName.replace('/', '.'));
        }
    }

    private void collectFromDescriptor(String descriptor) {
        int i = 0;
        while (i < descriptor.length()) {
            char c = descriptor.charAt(i);
            if (c == 'L') {
                int end = descriptor.indexOf(';', i + 1);
                if (end != -1) {
                    classes.add(descriptor.substring(i + 1, end).replace('/', '.'));
                    i = end + 1;
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }
    }

    private void collectFromClassDesc(ClassDesc desc) {
        if (desc.isArray()) {
            collectFromClassDesc(desc.componentType());
        } else if (!desc.isPrimitive()) {
            String descriptor = desc.descriptorString();
            if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                classes.add(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
            }
        }
    }

    private void collectFromMethodTypeDesc(MethodTypeDesc desc) {
        for (ClassDesc param : desc.parameterList()) {
            collectFromClassDesc(param);
        }
        collectFromClassDesc(desc.returnType());
    }

    private void collectFromSignature(String signature) {
        if (signature == null) return;
        Matcher m = SIGNATURE_CLASS_PATTERN.matcher(signature);
        while (m.find()) {
            classes.add(m.group(1).replace('/', '.'));
        }
    }

    public Set<String> getDependencies() {
        return classes;
    }
}
