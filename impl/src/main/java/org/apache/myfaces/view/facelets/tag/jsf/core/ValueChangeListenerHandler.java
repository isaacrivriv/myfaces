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
package org.apache.myfaces.view.facelets.tag.jsf.core;

import java.io.IOException;
import java.io.Serializable;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import javax.faces.view.EditableValueHolderAttachedObjectHandler;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributeException;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.util.lang.ClassUtils;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;

/**
 * Register an ValueChangeListener instance on the UIComponent associated with the closest parent UIComponent custom
 * action.
 * 
 * @author Jacob Hookom
 * @version $Id$
 */
@JSFFaceletTag(
        name = "f:valueChangeListener",
        bodyContent = "empty", 
        tagClass="org.apache.myfaces.taglib.core.ValueChangeListenerTag")
public final class ValueChangeListenerHandler extends TagHandler
    implements EditableValueHolderAttachedObjectHandler
{

    private static class LazyValueChangeListener implements ValueChangeListener, Serializable
    {
        private static final long serialVersionUID = 7613811124326963180L;

        private final String type;
        private final ValueExpression binding;

        public LazyValueChangeListener(String type, ValueExpression binding)
        {
            this.type = type;
            this.binding = binding;
        }

        @Override
        public void processValueChange(ValueChangeEvent event) throws AbortProcessingException
        {
            FacesContext facesContext = event.getFacesContext();
            if (facesContext == null)
            {
                return;
            }

            ValueChangeListener instance = null;
            if (this.binding != null)
            {
                instance = (ValueChangeListener) binding.getValue(facesContext.getELContext());
            }
            if (instance == null && this.type != null)
            {
                try
                {
                    instance = (ValueChangeListener) ClassUtils.forName(this.type).newInstance();
                }
                catch (Exception e)
                {
                    throw new AbortProcessingException("Couldn't Lazily instantiate ValueChangeListener", e);
                }
                if (this.binding != null)
                {
                    binding.setValue(facesContext.getELContext(), instance);
                }
            }
            if (instance != null)
            {
                instance.processValueChange(event);
            }
        }
    }

    private final TagAttribute binding;

    private final String listenerType;

    public ValueChangeListenerHandler(TagConfig config)
    {
        super(config);
        this.binding = this.getAttribute("binding");
        TagAttribute type = this.getAttribute("type");
        if (type != null)
        {
            if (!type.isLiteral())
            {
                throw new TagAttributeException(type, "Must be a literal class name of type ValueChangeListener");
            }
            else
            {
                // test it out
                try
                {
                    ClassUtils.forName(type.getValue());
                }
                catch (ClassNotFoundException e)
                {
                    throw new TagAttributeException(type, "Couldn't qualify ValueChangeListener", e);
                }
            }
            this.listenerType = type.getValue();
        }
        else
        {
            this.listenerType = null;
        }
    }

    /**
     * See taglib documentation.
     * 
     * See javax.faces.view.facelets.FaceletHandler#apply(javax.faces.view.facelets.FaceletContext, 
     * javax.faces.component.UIComponent)
     */
    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException
    {
        if (!ComponentHandler.isNew(parent))
        {
            return;
        }
        if (parent instanceof EditableValueHolder)
        {
            applyAttachedObject(ctx.getFacesContext(), parent);
        }
        else if (UIComponent.isCompositeComponent(parent))
        {
            FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance(ctx);
            mctx.addAttachedObjectHandler(parent, this);
        }
        else
        {
            throw new TagException(this.tag,
                    "Parent not composite component or an instance of EditableValueHolder: " + parent);
        }
    }

    @Override
    public void applyAttachedObject(FacesContext context, UIComponent parent)
    {
        // Retrieve the current FaceletContext from FacesContext object
        FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(
                FaceletContext.FACELET_CONTEXT_KEY);

        EditableValueHolder evh = (EditableValueHolder) parent;
        ValueExpression b = null;
        if (this.binding != null)
        {
            b = this.binding.getValueExpression(faceletContext, ValueChangeListener.class);
        }
        ValueChangeListener listener = new LazyValueChangeListener(this.listenerType, b);
        evh.addValueChangeListener(listener);
    }

    @JSFFaceletAttribute
    @Override
    public String getFor()
    {
        TagAttribute forAttribute = getAttribute("for");
        if (forAttribute == null)
        {
            return null;
        }

        return forAttribute.getValue();
    }

}
