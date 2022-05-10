import java.io.InputStream;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

class Solution {

    static class ValueStack extends Stack<Integer> {
    }

    static class Instructions extends ArrayList<Instruction> {
        public Instructions(Instructions instructions) {
            super(instructions);
        }

        public Instructions() {
            super();
        }
    }

    static class FunctionTable extends HashMap<String, Instructions> {
    }

    interface InstructionBody extends BiConsumer<ValueStack, FunctionTable> {
    }

    static class Instruction {
        final String name;
        final InstructionBody body;

        Instruction(String name, InstructionBody body) {
            this.name = name;
            this.body = body;
        }

        void execute(ValueStack stack, FunctionTable table) {
            body.accept(stack, table);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    interface InstructionFactory extends Function<Parser, Instruction> {
    }

    static class Tokens implements Iterator<String> {

        final Iterator<String> iterator;

        Tokens(final String[] tokens) {
            this.iterator = Arrays.asList(tokens).iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            return iterator.next();
        }
    }

    enum InstructionSet {
        ADD(parser ->
                new Instruction("Add", (stack, table) -> {
                    final int v1 = stack.pop();
                    final int v0 = stack.pop();
                    stack.push(v0 + v1);
                })
        ),
        SUB(parser ->
                new Instruction("Sub", (stack, table) -> {
                    final int v1 = stack.pop();
                    final int v0 = stack.pop();
                    stack.push(v0 - v1);
                })
        ),
        MUL(parser ->
                new Instruction("Mul", (stack, table) -> {
                    final int v1 = stack.pop();
                    final int v0 = stack.pop();
                    stack.push(v0 * v1);
                })
        ),
        DIV(parser ->
                new Instruction("Div", (stack, table) -> {
                    final int v1 = stack.pop();
                    final int v0 = stack.pop();
                    stack.push(v0 / v1);
                })
        ),
        MOD(parser ->
                new Instruction("Mod", (stack, table) -> {
                    final int v1 = stack.pop();
                    final int v0 = stack.pop();
                    stack.push(v0 % v1);
                })
        ),
        POP(parser ->
                new Instruction("Pop", (stack, table) -> stack.pop())
        ),
        DUP(parser ->
                new Instruction("Dup", (stack, table) -> {
                    final int v0 = stack.peek();
                    stack.push(v0);
                })
        ),
        SWP(parser ->
                new Instruction("Swp", (stack, table) -> {
                    final int v1 = stack.pop();
                    final int v0 = stack.pop();
                    stack.push(v1);
                    stack.push(v0);
                })
        ),
        ROT(parser ->
                new Instruction("Rot", (stack, table) -> {
                    final int v2 = stack.pop();
                    final int v1 = stack.pop();
                    final int v0 = stack.pop();
                    stack.push(v1);
                    stack.push(v2);
                    stack.push(v0);
                })
        ),
        OVR(parser ->
                new Instruction("Ovr", (stack, table) -> {
                    final int v0 = stack.pop();
                    final int v1 = stack.peek();
                    stack.push(v0);
                    stack.push(v1);
                })
        ),
        POS(parser ->
                new Instruction("Pos", (stack, table) -> {
                    final int value = stack.pop();
                    stack.push(value >= 0 ? 1 : 0);
                })
        ),
        NOT(parser ->
                new Instruction("Not", (stack, table) -> {
                    final int value = stack.pop();
                    stack.push(value == 0 ? 1 : 0);
                })
        ),
        OUT(parser ->
                new Instruction("Out", (stack, table) -> {
                    final int v0 = stack.pop();
                    System.out.println(v0);
                })
        ),
        DEF(parser -> {
            final String name = parser.tokens.next();
            parser.beginBlock();
            return new Instruction("Def " + name, (stack, table) -> {
            });
        }),
        END(parser -> {
            final Instructions instructions = parser.getInstructions();
            parser.endBlock();
            final Instruction def = instructions.remove(0);
            final String name = def.name.split(" ")[1]; // HACK
            return new Definition(name, instructions);
        }),
        IF(parser -> {
            parser.beginBlock();
            return new Instruction("If", (stack, table) -> {
            });
        }),
        ELS(parser -> {
            parser.beginBlock();
            return new Instruction("Els", (stack, table) -> {
            });
        }),
        FI(parser -> {
            final Instructions instructions = parser.getInstructions();
            parser.endBlock();
            final Instruction ifOrElse = instructions.remove(0);
            if (ifOrElse.name.equals("Els")) {
                final Instructions ifInstructions = parser.getInstructions();
                parser.endBlock();
                ifInstructions.remove(0);
                return new Conditional(ifInstructions, instructions);
            } else {
                return new Conditional(instructions);
            }
        });

        static class Call extends Instruction {
            Call(String name) {
                super("Call(" + name + ")", (stack, table) -> {
                    if (table.containsKey(name)) {
                        table.get(name).forEach(instruction -> instruction.execute(stack, table));
                    } else {
                        throw new RuntimeException(name);
                    }
                });
            }
        }

        static class Push extends Instruction {
            Push(final String value) {
                super("Push(" + value + ")", (stack, table) -> stack.push(Integer.parseInt(value)));
            }
        }

        static class Definition extends Instruction {
            private final Instructions instructions;

            Definition(String name, Instructions instructions) {
                super("Def(" + name + ")", (stack, table) -> table.put(name, instructions));
                this.instructions = instructions;
            }

            @Override
            public String toString() {
                return super.toString() + "\n\t" + instructions.toString();
            }
        }

        static class Conditional extends Instruction {
            private final Instructions affirmative;
            private final Instructions zero;

            Conditional(Instructions positive) {
                this("If", positive, new Instructions());
            }

            Conditional(Instructions affirmative, Instructions zero) {
                this("IfEls", affirmative, zero);
            }

            private Conditional(String name, Instructions affirmative, Instructions zero) {
                super(name, ((stack, table) -> {
                    if (stack.pop() == 0) {
                        zero.forEach(i -> i.execute(stack, table));
                    } else {
                        affirmative.forEach(i -> i.execute(stack, table));
                    }
                }));
                this.affirmative = affirmative;
                this.zero = zero;
            }

            @Override
            public String toString() {
                return super.toString() + affirmative.toString() + (zero.isEmpty() ? "" : "," + zero);
            }

        }

        final InstructionFactory factory;

        InstructionSet(final InstructionFactory factory) {
            this.factory = factory;
        }

        static Optional<Instruction> get(final String token, final Parser parser) {
            final Optional<InstructionSet> value = Arrays.stream(InstructionSet.values())
                    .filter(v -> v.name().equals(token))
                    .findAny();
            return value.map(v -> v.factory.apply(parser));
        }

    }

    static class Parser {
        private final Tokens tokens;
        private final Stack<Instructions> instructionStack = new Stack<>();

        private Instructions instructions;

        Parser(final Tokens tokens) {
            this.tokens = tokens;
        }

        void parse(final String token) {
            if (Character.isAlphabetic(token.codePointAt(0))) {
                final Optional<Instruction> instruction = InstructionSet.get(token, this);
                this.addInstruction(instruction.isEmpty()
                        ? new InstructionSet.Call(token)
                        : instruction.get()
                );
            } else {
                this.addInstruction(new InstructionSet.Push(token));
            }
        }


        void addInstruction(Instruction instruction) {
            instructions.add(instruction);
        }

        Instructions getInstructions() {
            return new Instructions(instructions);
        }

        void beginBlock() {
            instructionStack.push(instructions);
            this.instructions = new Instructions();
        }

        // Before this getInstructions is always called
        void endBlock() {
            this.instructions = instructionStack.pop();
        }

    }

    static class Interpreter {

        final ValueStack stack = new ValueStack();
        final FunctionTable functionTable = new FunctionTable();

        Instructions process(final String line) {
            System.err.println(line);
            final Tokens tokens = new Tokens(line.split(" "));
            final Instructions instructions = evaluate(tokens);
            instructions.forEach(System.err::println);
            instructions.forEach(i -> i.execute(stack, functionTable));
            return instructions;
        }

        Instructions evaluate(final Tokens tokens) {
            final Parser parser = new Parser(tokens);
            parser.beginBlock();
            while (tokens.hasNext()) {
                final String token = tokens.next();
                parser.parse(token);
            }
            final Instructions instructions = parser.getInstructions();
            parser.endBlock();
            return instructions;
        }

    }

    public static void main(String args[]) {
        final InputStream in = System.in;
        new Solution().run(in);
    }

    public void run(InputStream input) {
        final Scanner in = new Scanner(input);

        final int count = in.nextInt();
        in.nextLine(); // empty

        final List<String> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final String line = in.nextLine();
            System.err.println(line);
            lines.add(line);
        }
        System.err.println("===========================");

        final String program = lines.stream()
                .map(String::trim)
                .collect(Collectors.joining(" "));
        final Interpreter interpreter = new Interpreter();
        interpreter.process(program);
    }
}