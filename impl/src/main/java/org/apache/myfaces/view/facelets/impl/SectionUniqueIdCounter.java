/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Hierarchical counter to generate unique ids.
 * 
 * @author Leonardo Uribe
 *
 */
public class SectionUniqueIdCounter
{
    private List<Section> _counterStack;
    
    private int _activeSection;
    
    private final String _prefix;
    
    private final StringBuilder _builder;
    
    private char[] _bufferConversion;
    
    private final int _radix;
   
    public SectionUniqueIdCounter()
    {
        _activeSection = 0;
        _radix = Character.MAX_RADIX;
        _counterStack = new ArrayList<Section>();
        _counterStack.add(new Section(null,1,_radix));
        _prefix = null;
        _builder = new StringBuilder(30);
        _bufferConversion = new char[15];
    }
    
    public SectionUniqueIdCounter(String prefix)
    {
        _activeSection = 0;
        _radix = Character.MAX_RADIX;
        _counterStack = new ArrayList<Section>();
        _counterStack.add(new Section(null,1,_radix));
        _prefix = prefix;
        _builder = new StringBuilder(30);
        _bufferConversion = new char[15];
    }
    
    public SectionUniqueIdCounter(String prefix, int radix)
    {
        _activeSection = 0;
        _radix = radix;
        _counterStack = new ArrayList<Section>();
        _counterStack.add(new Section(null,1,_radix));
        _prefix = prefix;
        _builder = new StringBuilder(30);
        _bufferConversion = new char[15];
    }

    public String startUniqueIdSection()
    {
        //1. Calculate prefix
        _builder.delete(0, _builder.length());
        boolean added = false;
        for (int i = 0; i < _counterStack.size(); i++)
        {
            if (added)
            {
                _builder.append('_');
            }
            //_builder.append(Long.toString(_counterStack.get(i).getCounter(), _radix));
            appendToBuilder(_counterStack.get(i).getCounter(), _radix, _builder, _bufferConversion);
            added = true;
        }
        
        _counterStack.add(new Section(_builder.toString(),1,_radix));
        _activeSection++;
        return _builder.toString();
    }
    
    public String startUniqueIdSection(String base)
    {
        //1. Calculate prefix
        _builder.delete(0, _builder.length());
        boolean added = false;
        for (int i = 0; i < _counterStack.size(); i++)
        {
            if (added)
            {
                _builder.append('_');
            }
            //_builder.append(Long.toString(_counterStack.get(i).getCounter(), _radix));
            appendToBuilder(_counterStack.get(i).getCounter(), _radix, _builder, _bufferConversion);
            added = true;
        }
        if (base != null && base.length() > 0)
        {
            _builder.append('_');
            _builder.append(base);
        }
        _counterStack.add(new Section(_builder.toString(),1,_radix));
        _activeSection++;
        return _builder.toString();
    }

    public String generateUniqueId()
    {
        return _counterStack.get(_activeSection).generateUniqueId(_prefix);
    }
    
    public void generateUniqueId(StringBuilder builderToAdd)
    {
        _counterStack.get(_activeSection).generateUniqueId(_prefix, builderToAdd);
    }
    
    public void endUniqueIdSection()
    {
        if (_activeSection <= 0)
        {
            _counterStack.get(_activeSection).generateUniqueId(_prefix);
            return;
        }
        else
        {
            _counterStack.remove(_activeSection);
            _activeSection--;
            _counterStack.get(_activeSection).generateUniqueId(_prefix);
        }
    }
    
    private static class Section
    {
        
        private String prefix;
        private long counter;
        private final StringBuilder _builder;
        private char[] _bufferConversion;
        private final int _radix;
        
        public Section(String prefix, long counter, int radix)
        {
            super();
            this.prefix = prefix;
            this.counter = counter;
            _builder = new StringBuilder(30);
            _bufferConversion = new char[15];
            _radix = radix;
        }

        public long getCounter()
        {
            return counter;
        }

        public void generateUniqueId(String base, StringBuilder builder)
        {
            long i = this.counter;
            this.counter++;
            //_builder.delete(0, _builder.length());
            if (base != null)
            {
                builder.append(base);
            }
            if (this.prefix != null)
            {
                builder.append(this.prefix);
                builder.append('_');
            }
            // By performance reasons, Long.toString is a very expensive
            // operation in this location, because it triggers a new String()
            //_builder.append(Long.toString(i, _radix));
            appendToBuilder(i, _radix, builder, _bufferConversion);
            //return _builder.toString();
        }
        
        public String generateUniqueId(String base)
        {
            long i = this.counter;
            this.counter++;
            _builder.delete(0, _builder.length());
            if (base != null)
            {
                _builder.append(base);
            }
            if (this.prefix != null)
            {
                _builder.append(this.prefix);
                _builder.append('_');
            }
            // By performance reasons, Long.toString is a very expensive
            // operation in this location, because it triggers a new String()
            //_builder.append(Long.toString(i, _radix));
            appendToBuilder(i, _radix, _builder, _bufferConversion);
            return _builder.toString();
        }
    }
    
    //From Harmony Long.toString(l,radix)
    private static void appendToBuilder(long l, int radix, StringBuilder builder, char[] bufferConversion)
    {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
        {
            radix = 10;
        }
        if (l == 0)
        {
            builder.append('0');
            return;
        }

        int count = 2;
        long j = l;
        boolean negative = l < 0;
        if (!negative)
        {
            count = 1;
            j = -l;
        }
        while ((l /= radix) != 0)
        {
            count++;
        }

        if (bufferConversion.length < count)
        {
            bufferConversion = new char[count];
        }
        int finalCount = count;

        char[] buffer = bufferConversion;
        do 
        {
            int ch = 0 - (int) (j % radix);
            if (ch > 9)
            {
                ch = ch - 10 + 'a';
            }
            else
            {
                ch += '0';
            }
            buffer[--count] = (char) ch;
            j /= radix;
        }
        while (j != 0);
        if (negative)
        {
            buffer[0] = '-';
        }
        for (int i = 0; i < finalCount; i++)
        {
            builder.append(buffer[i]);
        }
        //return new String(0, buffer.length, buffer);
    }
}
