/*
 * Copyright 2016 the original author or authors.
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

package capital.scalable.restdocs.jackson;

import static capital.scalable.restdocs.constraints.ConstraintReader.CONSTRAINTS_ATTRIBUTE;
import static capital.scalable.restdocs.constraints.ConstraintReader.OPTIONAL_ATTRIBUTE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import capital.scalable.restdocs.constraints.ConstraintReader;
import capital.scalable.restdocs.javadoc.JavadocReader;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.junit.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.snippet.Attributes.Attribute;

public class FieldDocumentationGeneratorTest {

    @Test
    public void testGenerateDocumentationForPrimitiveTypes() throws Exception {
        // given
        ObjectMapper mapper = createMapper();
        JavadocReader javadocReader = mock(JavadocReader.class);
        when(javadocReader.resolveFieldComment(PrimitiveTypes.class, "stringField"))
                .thenReturn("A string");
        when(javadocReader.resolveFieldComment(PrimitiveTypes.class, "booleanField"))
                .thenReturn("A boolean");
        when(javadocReader.resolveFieldComment(PrimitiveTypes.class, "numberField1"))
                .thenReturn("An integer");
        when(javadocReader.resolveFieldComment(PrimitiveTypes.class, "numberField2"))
                .thenReturn("A decimal");

        ConstraintReader constraintReader = mock(ConstraintReader.class);

        FieldDocumentationGenerator generator =
                new FieldDocumentationGenerator(mapper.writer(), javadocReader, constraintReader);
        Type type = PrimitiveTypes.class;

        // when
        List<ExtendedFieldDescriptor> fieldDescriptions = cast(generator
                .generateDocumentation(type, mapper.getTypeFactory()));
        // then
        assertThat(fieldDescriptions.size(), is(4));
        assertThat(fieldDescriptions.get(0),
                is(descriptor("stringField", "String", "A string", "true")));
        assertThat(fieldDescriptions.get(1),
                is(descriptor("booleanField", "Boolean", "A boolean", "true")));
        assertThat(fieldDescriptions.get(2),
                is(descriptor("numberField1", "Integer", "An integer", "true")));
        assertThat(fieldDescriptions.get(3),
                is(descriptor("numberField2", "Decimal", "A decimal", "true")));
    }

    @Test
    public void testGenerateDocumentationForEnum() throws Exception {
        // given
        ObjectMapper mapper = new ObjectMapper();
        // includes test for preventing duplicate enum description, because we are inspecting
        // getter and field for annotations
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC));

        JavadocReader javadocReader = mock(JavadocReader.class);
        when(javadocReader.resolveFieldComment(EnumType.class, "enumField1"))
                .thenReturn("An enum");
        when(javadocReader.resolveFieldComment(EnumType.class, "enumField2"))
                .thenReturn("An enum");

        ConstraintReader constraintReader = mock(ConstraintReader.class);
        when(constraintReader.getConstraintMessages(EnumType.class, "enumField2"))
                .thenReturn(singletonList("Must not be null"));

        FieldDocumentationGenerator generator =
                new FieldDocumentationGenerator(mapper.writer(), javadocReader, constraintReader);
        Type type = EnumType.class;

        // when
        List<ExtendedFieldDescriptor> fieldDescriptions = cast(generator
                .generateDocumentation(type, mapper.getTypeFactory()));
        // then
        assertThat(fieldDescriptions.size(), is(2));
        assertThat(fieldDescriptions.get(0),
                is(descriptor("enumField1", "String", "An enum", "true",
                        "Must be one of [ONE, TWO]")));
        assertThat(fieldDescriptions.get(1),
                is(descriptor("enumField2", "String", "An enum", "true",
                        "Must not be null", "Must be one of [A, B]")));
    }

    @Test
    public void testGenerateDocumentationForComposedTypes() throws Exception {
        // given
        ObjectMapper mapper = createMapper();
        JavadocReader javadocReader = mock(JavadocReader.class);
        when(javadocReader.resolveFieldComment(ComposedTypes.class, "objectField"))
                .thenReturn("An object");
        when(javadocReader.resolveFieldComment(PrimitiveTypes.class, "stringField"))
                .thenReturn("A string");
        when(javadocReader.resolveFieldComment(PrimitiveTypes.class, "booleanField"))
                .thenReturn("A boolean");
        when(javadocReader.resolveFieldComment(PrimitiveTypes.class, "numberField1"))
                .thenReturn("An integer");
        when(javadocReader.resolveFieldComment(PrimitiveTypes.class, "numberField2"))
                .thenReturn("A decimal");
        when(javadocReader.resolveFieldComment(ComposedTypes.class, "arrayField"))
                .thenReturn("An array");

        ConstraintReader constraintReader = mock(ConstraintReader.class);

        FieldDocumentationGenerator generator =
                new FieldDocumentationGenerator(mapper.writer(), javadocReader, constraintReader);
        Type type = ComposedTypes.class;

        // when
        List<ExtendedFieldDescriptor> fieldDescriptions = cast(generator
                .generateDocumentation(type, mapper.getTypeFactory()));
        // then
        assertThat(fieldDescriptions.size(), is(10));
        assertThat(fieldDescriptions.get(0),
                is(descriptor("objectField", "Object", "An object", "true")));
        assertThat(fieldDescriptions.get(1),
                is(descriptor("objectField.stringField", "String", "A string", "true")));
        assertThat(fieldDescriptions.get(2),
                is(descriptor("objectField.booleanField", "Boolean", "A boolean", "true")));
        assertThat(fieldDescriptions.get(3),
                is(descriptor("objectField.numberField1", "Integer", "An integer", "true")));
        assertThat(fieldDescriptions.get(4),
                is(descriptor("objectField.numberField2", "Decimal", "A decimal", "true")));
        assertThat(fieldDescriptions.get(5),
                is(descriptor("arrayField", "Array", "An array", "true")));
        assertThat(fieldDescriptions.get(6),
                is(descriptor("arrayField[].stringField", "String", "A string", "true")));
        assertThat(fieldDescriptions.get(7),
                is(descriptor("arrayField[].booleanField", "Boolean", "A boolean", "true")));
        assertThat(fieldDescriptions.get(8),
                is(descriptor("arrayField[].numberField1", "Integer", "An integer", "true")));
        assertThat(fieldDescriptions.get(9),
                is(descriptor("arrayField[].numberField2", "Decimal", "A decimal", "true")));
    }

    @Test
    public void testGenerateDocumentationForNestedTypes() throws Exception {
        // given
        ObjectMapper mapper = createMapper();
        JavadocReader javadocReader = mock(JavadocReader.class);
        when(javadocReader.resolveFieldComment(FirstLevel.class, "second"))
                .thenReturn("2nd level");
        when(javadocReader.resolveFieldComment(SecondLevel.class, "third"))
                .thenReturn("3rd level");
        when(javadocReader.resolveFieldComment(ThirdLevel.class, "fourth"))
                .thenReturn("4th level");
        when(javadocReader.resolveFieldComment(FourthLevel.class, "fifth"))
                .thenReturn("5th level");
        when(javadocReader.resolveFieldComment(FifthLevel.class, "last"))
                .thenReturn("An integer");

        ConstraintReader constraintReader = mock(ConstraintReader.class);

        FieldDocumentationGenerator generator =
                new FieldDocumentationGenerator(mapper.writer(), javadocReader, constraintReader);
        Type type = FirstLevel.class;

        // when
        List<ExtendedFieldDescriptor> fieldDescriptions = cast(generator
                .generateDocumentation(type, mapper.getTypeFactory()));

        // then
        assertThat(fieldDescriptions.size(), is(5));
        assertThat(fieldDescriptions.get(0),
                is(descriptor("second", "Object", "2nd level", "true")));
        assertThat(fieldDescriptions.get(1),
                is(descriptor("second.third", "Array", "3rd level", "true")));
        assertThat(fieldDescriptions.get(2),
                is(descriptor("second.third[].fourth", "Object", "4th level", "true")));
        assertThat(fieldDescriptions.get(3),
                is(descriptor("second.third[].fourth.fifth", "Array", "5th level", "true")));
        assertThat(fieldDescriptions.get(4),
                is(descriptor("second.third[].fourth.fifth[].last", "Integer", "An integer",
                        "true")));
    }

    @Test
    public void testGenerateDocumentationForRecursiveTypes() throws Exception {
        // given
        ObjectMapper mapper = createMapper();
        JavadocReader javadocReader = mock(JavadocReader.class);
        when(javadocReader.resolveFieldComment(RecursiveType.class, "value"))
                .thenReturn("Type value");
        when(javadocReader.resolveFieldComment(RecursiveType.class, "children"))
                .thenReturn("Child types");
        when(javadocReader.resolveFieldComment(RecursiveType.class, "sibling"))
                .thenReturn("Sibling type");

        ConstraintReader constraintReader = mock(ConstraintReader.class);

        FieldDocumentationGenerator generator =
                new FieldDocumentationGenerator(mapper.writer(), javadocReader, constraintReader);
        Type type = RecursiveType.class;

        // when
        List<ExtendedFieldDescriptor> fieldDescriptions = cast(generator
                .generateDocumentation(type, mapper.getTypeFactory()));

        // then
        assertThat(fieldDescriptions.size(), is(3));
        assertThat(fieldDescriptions.get(0),
                is(descriptor("value", "String", "Type value", "true")));
        assertThat(fieldDescriptions.get(1),
                is(descriptor("children", "Array", "Child types", "true")));
        assertThat(fieldDescriptions.get(2),
                is(descriptor("sibling", "Object", "Sibling type", "true")));
    }

    @Test
    public void testGenerateDocumentationForExternalSerializer() throws Exception {
        // given
        ObjectMapper mapper = createMapper();

        JavadocReader javadocReader = mock(JavadocReader.class);
        when(javadocReader.resolveFieldComment(ExternalSerializer.class, "bigDecimal"))
                .thenReturn("A decimal");

        ConstraintReader constraintReader = mock(ConstraintReader.class);

        SimpleModule testModule = new SimpleModule("TestModule");
        testModule.addSerializer(new BigDecimalSerializer());
        mapper.registerModule(testModule);

        FieldDocumentationGenerator generator =
                new FieldDocumentationGenerator(mapper.writer(), javadocReader, constraintReader);
        Type type = ExternalSerializer.class;

        // when
        List<ExtendedFieldDescriptor> fieldDescriptions = cast(generator
                .generateDocumentation(type, mapper.getTypeFactory()));

        // then
        assertThat(fieldDescriptions.size(), is(1));
        assertThat(fieldDescriptions.get(0),
                is(descriptor("bigDecimal", "Decimal", "A decimal", "true")));
    }

    @Test
    public void testGenerateDocumentationForJacksonAnnotations() throws Exception {
        // given
        ObjectMapper mapper = createMapper();

        JavadocReader javadocReader = mock(JavadocReader.class);
        when(javadocReader.resolveFieldComment(JsonAnnotations.class, "location"))
                .thenReturn("A location");
        when(javadocReader.resolveFieldComment(JsonAnnotations.class, "uri"))
                .thenReturn("A uri");
        when(javadocReader.resolveMethodComment(JsonAnnotations.class, "getParameter"))
                .thenReturn("A parameter");
        when(javadocReader.resolveFieldComment(JsonAnnotations.Meta.class, "headers"))
                .thenReturn("A header map");

        ConstraintReader constraintReader = mock(ConstraintReader.class);

        FieldDocumentationGenerator generator =
                new FieldDocumentationGenerator(mapper.writer(), javadocReader, constraintReader);
        Type type = JsonAnnotations.class;

        // when
        List<ExtendedFieldDescriptor> fieldDescriptions = cast(generator
                .generateDocumentation(type, mapper.getTypeFactory()));

        // then
        assertThat(fieldDescriptions.size(), is(4));
        // @JsonPropertyOrder puts it to first place
        assertThat(fieldDescriptions.get(0),
                is(descriptor("uri", "String", "A uri", "true")));
        // @JsonProperty
        assertThat(fieldDescriptions.get(1),
                is(descriptor("path", "String", "A location", "true")));
        // @JsonGetter
        assertThat(fieldDescriptions.get(2),
                is(descriptor("param", "String", "A parameter", "true")));
        // @JsonUnwrapped
        assertThat(fieldDescriptions.get(3),
                is(descriptor("headers", "Map", "A header map", "true")));
    }

    @Test
    public void testGenerateDocumentationForFieldResolution() throws Exception {
        // given
        // different mapper for custom field resolution
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY));

        ConstraintReader constraintReader = mock(ConstraintReader.class);

        JavadocReader javadocReader = mock(JavadocReader.class);
        // comment on field directly
        when(javadocReader.resolveFieldComment(FieldCommentResolution.class, "location"))
                .thenReturn("A location");
        // comment on getter instead of field
        when(javadocReader.resolveMethodComment(FieldCommentResolution.class, "getType"))
                .thenReturn("A type");
        // comment on field instead of getter
        when(javadocReader.resolveFieldComment(FieldCommentResolution.class, "uri"))
                .thenReturn("A uri");
        when(javadocReader.resolveFieldComment(FieldCommentResolution.class, "secured"))
                .thenReturn("A secured flag");

        FieldDocumentationGenerator generator =
                new FieldDocumentationGenerator(mapper.writer(), javadocReader, constraintReader);
        Type type = FieldCommentResolution.class;

        // when
        List<ExtendedFieldDescriptor> fieldDescriptions = cast(generator
                .generateDocumentation(type, mapper.getTypeFactory()));

        // then
        assertThat(fieldDescriptions.size(), is(4));
        // field comment
        assertThat(fieldDescriptions.get(0),
                is(descriptor("location", "String", "A location", "true")));
        // getter comment
        assertThat(fieldDescriptions.get(1),
                is(descriptor("type", "String", "A type", "true")));
        // field comment
        assertThat(fieldDescriptions.get(2),
                is(descriptor("uri", "String", "A uri", "true")));
        assertThat(fieldDescriptions.get(3),
                is(descriptor("secured", "Boolean", "A secured flag", "true")));
    }

    @Test
    public void testGenerateDocumentationForConstraints() throws Exception {
        // given
        ObjectMapper mapper = createMapper();

        JavadocReader javadocReader = mock(JavadocReader.class);

        ConstraintReader constraintReader = mock(ConstraintReader.class);
        when(constraintReader.isMandatory(NotNull.class)).thenReturn(true);
        when(constraintReader.isMandatory(NotEmpty.class)).thenReturn(true);
        when(constraintReader.isMandatory(NotBlank.class)).thenReturn(true);

        when(constraintReader.getConstraintMessages(ConstraintResolution.class, "location"))
                .thenReturn(singletonList("A constraint for location"));
        when(constraintReader.getConstraintMessages(ConstraintResolution.class, "type"))
                .thenReturn(singletonList("A constraint for type"));
        when(constraintReader.getOptionalMessages(ConstraintResolution.class, "type"))
                .thenReturn(singletonList("false"));
        when(constraintReader.getOptionalMessages(ConstraintResolution.class, "params"))
                .thenReturn(singletonList("false"));
        when(constraintReader.getConstraintMessages(ConstraintField.class, "value"))
                .thenReturn(asList(
                        new String[]{"A constraint1 for value", "A constraint2 for value"}));
        when(constraintReader.getOptionalMessages(ConstraintField.class, "value"))
                .thenReturn(singletonList("false"));

        FieldDocumentationGenerator generator =
                new FieldDocumentationGenerator(mapper.writer(), javadocReader, constraintReader);
        Type type = ConstraintResolution.class;

        // when
        List<ExtendedFieldDescriptor> fieldDescriptions = cast(generator
                .generateDocumentation(type, mapper.getTypeFactory()));

        // then
        assertThat(fieldDescriptions.size(), is(5));
        assertThat(fieldDescriptions.get(0),
                is(descriptor("location", "String", null, "true", "A constraint for location")));
        assertThat(fieldDescriptions.get(1),
                is(descriptor("type", "Integer", null, "false", "A constraint for type")));
        assertThat(fieldDescriptions.get(2),
                is(descriptor("params", "Array", null, "false")));
        assertThat(fieldDescriptions.get(3),
                is(descriptor("params[].value", "String", null, "false",
                        "A constraint1 for value", "A constraint2 for value")));
        assertThat(fieldDescriptions.get(4),
                is(descriptor("flags", "Array", null, "true")));
    }

    private ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        return mapper;
    }

    private ExtendedFieldDescriptor descriptor(String path, Object fieldType,
            String comment, String optional, String... constraints) {
        FieldDescriptor fieldDescriptor = fieldWithPath(path)
                .type(fieldType)
                .description(comment);
        fieldDescriptor.attributes(
                new Attribute(OPTIONAL_ATTRIBUTE, Arrays.asList(optional)));
        if (constraints != null) {
            fieldDescriptor.attributes(
                    new Attribute(CONSTRAINTS_ATTRIBUTE, Arrays.asList(constraints)));
        }
        return new ExtendedFieldDescriptor(fieldDescriptor);
    }

    private List<ExtendedFieldDescriptor> cast(List<FieldDescriptor> original) {
        List<ExtendedFieldDescriptor> casted = new ArrayList<>(original.size());
        for (FieldDescriptor d : original) {
            casted.add(new ExtendedFieldDescriptor(d));
        }
        return casted;
    }

    private static class PrimitiveTypes {
        private String stringField;
        private Boolean booleanField;
        private Integer numberField1;
        private Double numberField2;
    }

    private static class ComposedTypes {
        private PrimitiveTypes objectField;
        private List<PrimitiveTypes> arrayField;
    }

    private static class FirstLevel {
        private SecondLevel second;
    }

    private static class SecondLevel {
        private List<ThirdLevel> third;
    }

    private static class ThirdLevel {
        private FourthLevel fourth;
    }

    private static class FourthLevel {
        private List<FifthLevel> fifth;
    }

    private static class FifthLevel {
        private Integer last;
    }

    private static class ExternalSerializer {
        private BigDecimal bigDecimal;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL) // does not affect field resolution
    @JsonIgnoreProperties({"value"})
    @JsonPropertyOrder({"uri", "path"})
    private static class JsonAnnotations {
        @JsonProperty("path")
        private String location;

        @JsonIgnore
        private String type;

        private String value;

        private String uri;

        private String param;

        @JsonUnwrapped
        private Meta meta;

        @JsonGetter("param")
        public String getParameter() {
            return param;
        }

        private class Meta {
            private Map<String, String> headers;
        }
    }

    private static class FieldCommentResolution {
        public String location; // doc is here
        public String type;
        private String uri; // doc is here
        private boolean secured;// doc is here

        public String getType() { // doc is here
            return type;
        }

        public String getUri() {
            return uri;
        }

        public boolean isSecured() {
            return secured;
        }
    }

    private static class ConstraintResolution {
        @Length(min = 1, max = 255)
        private String location;
        @NotNull
        @Min(1)
        @Max(10)
        private Integer type;
        @Valid
        @NotEmpty
        private List<ConstraintField> params;
        private List<Boolean> flags;
    }

    private static class ConstraintField {
        @NotBlank
        @Size(max = 20)
        private String value;
    }

    private static class RecursiveType {
        private String value;
        @RestdocsNotExpanded
        private List<RecursiveType> children;
        @RestdocsNotExpanded
        private RecursiveType sibling;
    }

    private static class EnumType {
        // directly from field
        protected SomeEnum12 enumField1;
        private SomeEnumAB enumField2;

        // from getter, then field
        public SomeEnumAB getEnumField2() {
            return enumField2;
        }
    }

    private enum SomeEnum12 {
        ONE, TWO
    }

    private enum SomeEnumAB {
        A, B
    }
}
