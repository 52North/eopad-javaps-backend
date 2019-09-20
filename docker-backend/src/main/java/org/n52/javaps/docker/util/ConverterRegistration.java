/*
 * Copyright 2019 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.javaps.docker.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Component
public class ConverterRegistration {

    private Set<Converter<?, ?>> converters = Collections.emptySet();
    private Set<ConverterFactory<?, ?>> converterFactories = Collections.emptySet();
    private Set<GenericConverter> genericConverters = Collections.emptySet();
    private ConverterRegistry converterRegistry;

    @TypeConverter
    @Autowired(required = false)
    public void setConverters(Set<Converter<?, ?>> converters) {
        this.converters = Optional.ofNullable(converters).orElseGet(Collections::emptySet);
    }

    @TypeConverter
    @Autowired(required = false)
    public void setConverterFactories(Set<ConverterFactory<?, ?>> converterFactories) {
        this.converterFactories = Optional.ofNullable(converterFactories).orElseGet(Collections::emptySet);
    }

    @TypeConverter
    @Autowired(required = false)
    public void setGenericConverters(Set<GenericConverter> genericConverters) {
        this.genericConverters = Optional.ofNullable(genericConverters).orElseGet(Collections::emptySet);
    }

    @Autowired
    public void setConverterRegistry(ConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    @PostConstruct
    public void registerConverter() {
        if (converterRegistry != null) {
            converters.forEach(converterRegistry::addConverter);
            genericConverters.forEach(converterRegistry::addConverter);
            converterFactories.forEach(converterRegistry::addConverterFactory);
        }
    }

}
